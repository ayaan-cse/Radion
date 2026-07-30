package com.radion.service.pipeline.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.EventCategory;
import com.radion.domain.enums.MessageProcessingState;
import com.radion.domain.models.ClassroomAIProcessingLog;
import com.radion.domain.models.ClassroomAnnouncement;
import com.radion.domain.models.ClassroomCourseWork;
import com.radion.domain.models.Event;
import com.radion.domain.models.Task;
import com.radion.domain.models.User;
import com.radion.repository.ClassroomAIProcessingLogRepository;
import com.radion.repository.ClassroomAnnouncementRepository;
import com.radion.repository.ClassroomCourseWorkRepository;
import com.radion.repository.EventRepository;
import com.radion.repository.TaskRepository;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.engine.EventEngine;
import com.radion.service.pipeline.ai.providers.GeminiClassroomReasoningProvider;
import com.radion.service.pipeline.models.ClassroomReasoningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomPipelineOrchestrator {

    private final ClassroomCourseWorkRepository courseWorkRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomAIProcessingLogRepository aiProcessingLogRepository;
    private final GeminiClassroomReasoningProvider geminiProvider;
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final GoogleCalendarSyncService googleCalendarSyncService;
    private final EventEngine eventEngine;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // COURSEWORK PIPELINE
    // =========================================================================

    /**
     * Entry point for a newly fetched ClassroomCourseWork item.
     * Splitting transactions so API calls aren't wrapped in @Transactional.
     */
    public void processCourseWork(ClassroomCourseWork courseWork) {
        if (courseWork.getProcessingState() == MessageProcessingState.AI_PROCESSED) {
            log.info("CourseWork is already processed.");
            return;
        }

        log.info("Starting Classroom Pipeline for CourseWork: {}", courseWork.getTitle());

        ClassroomReasoningResponse reasoningResult;

        if (courseWork.getDueDate() != null && courseWork.getTitle() != null) {
            log.info("CourseWork already has due date and title, bypassing Gemini");
            reasoningResult = ClassroomReasoningResponse.builder()
                .isActionRequired(true)
                .type("ASSIGNMENT")
                .topic(courseWork.getTitle())
                .summary(courseWork.getDescription() != null && !courseWork.getDescription().isEmpty()
                    ? courseWork.getDescription().substring(0, Math.min(courseWork.getDescription().length(), 150)) + "..."
                    : "Classroom Assignment")
                .build();
        } else {
            try {
                reasoningResult = geminiProvider.evaluateCourseWork(courseWork);
            } catch (Exception e) {
                // If it fails, save it first so the retry scheduler can pick it up
                if (courseWork.getId() == null) {
                    courseWork = courseWorkRepository.save(courseWork);
                }
                handleCourseWorkFailure(courseWork, e);
                return;
            }
        }

        // EVALUATE IN MEMORY BEFORE PERSISTING
        boolean isNonActionable = "MATERIAL".equals(reasoningResult.getType()) || "NOTES".equals(reasoningResult.getType());

        if (isNonActionable || courseWork.getDueDate() == null) {
            log.info("CourseWork '{}' is non-actionable. Discarding immediately without persisting.", courseWork.getTitle());
            if (courseWork.getId() != null) {
                aiProcessingLogRepository.findByCourseWorkId(courseWork.getId())
                        .ifPresent(aiProcessingLogRepository::delete);
                courseWorkRepository.delete(courseWork);
            }
            return;
        }

        // ACTIONABLE - Save to database now if it's new
        if (courseWork.getId() == null) {
            courseWork = courseWorkRepository.save(courseWork);
        }

        try {
            commitCourseWorkTransaction(courseWork, reasoningResult);
            // Sync calendar OUTSIDE of the transaction
            syncCourseWorkCalendarEvent(courseWork);
        } catch (Exception e) {
            log.error("Failed to save reasoning for CourseWork: {}", courseWork.getId(), e);
            handleCourseWorkFailure(courseWork, e);
        }
    }

    @Transactional
    protected void commitCourseWorkTransaction(ClassroomCourseWork courseWork, ClassroomReasoningResponse result) {
        log.info("Committing Classroom Reasoning for CourseWork: {}", courseWork.getId());

        saveAIProcessingLog(courseWork, result);

        // Store AI-classified type
        if (result.getType() != null) {
            courseWork.setType(result.getType());
        }

        User user = courseWork.getUser();
        String businessKey = "CLASSROOM_COURSEWORK_" + courseWork.getId();

        // Determine EventCategory from AI-classified type
        EventCategory eventCategory = resolveEventCategory(result.getType());

        // UPSERT Task
        Task task = taskRepository.findByBusinessKey(businessKey).orElse(new Task());
        task.setUser(user);
        task.setTitle(courseWork.getTitle() + (result.getTopic() != null ? " - " + result.getTopic() : ""));
        task.setSource("GOOGLE_CLASSROOM");
        task.setDueDate(courseWork.getDueDate());
        task.setBusinessKey(businessKey);
        if (task.getId() == null) {
            task.setCompleted(false);
        }
        taskRepository.save(task);

        // UPSERT Event (only if there's a due date)
        if (courseWork.getDueDate() != null) {
            Event event = eventRepository.findBySourceCourseWorkId(courseWork.getId()).orElse(new Event());
            event.setUser(user);
            event.setSourceCourseWork(courseWork);
            event.setTitle(courseWork.getTitle());
            event.setCompanyOrSource(courseWork.getCourse().getName());
            event.setCategory(eventCategory);
            event.setEventTime(courseWork.getDueDate());
            event.setTimelineGroupId("CLASSROOM_" + courseWork.getCourse().getId());
            if (event.getCalendarSyncStatus() == null) {
                event.setCalendarSyncStatus("PENDING");
            }
            eventRepository.save(event);
        }

        courseWork.setProcessingState(MessageProcessingState.AI_PROCESSED);
        courseWorkRepository.save(courseWork);
    }

    private void syncCourseWorkCalendarEvent(ClassroomCourseWork courseWork) {
        Optional<Event> eventOpt = eventRepository.findBySourceCourseWorkId(courseWork.getId());
        if (eventOpt.isEmpty()) return;

        Event event = eventOpt.get();
        User user = courseWork.getUser();

        CalendarEventDTO gcalDto = CalendarEventDTO.builder()
                .eventId(event.getId().toString())
                .title("Classroom: " + event.getTitle())
                .description("Course: " + event.getCompanyOrSource()
                        + (courseWork.getDescription() != null ? "\n\n" + courseWork.getDescription() : ""))
                .location("")
                .companyName(event.getCompanyOrSource())
                .category(event.getCategory())
                .startTime(event.getEventTime())
                .endTime(event.getEventTime().plusHours(1))
                .requiresReminders(true)
                .build();

        try {
            String gCalId;
            if (event.getGoogleCalendarEventId() != null) {
                gCalId = googleCalendarSyncService.updateEvent(user, event.getGoogleCalendarEventId(), gcalDto);
            } else {
                gCalId = googleCalendarSyncService.syncEvent(user, gcalDto);
            }
            eventEngine.updateCalendarSyncStatus(event.getId(), gCalId, "SYNCED", null);
        } catch (Exception e) {
            log.warn("Calendar sync failed for Classroom CourseWork Event {}: {}", event.getId(), e.getMessage());
            eventEngine.updateCalendarSyncStatus(event.getId(), null, "FAILED", e);
        }
    }

    // =========================================================================
    // ANNOUNCEMENT PIPELINE
    // =========================================================================

    /**
     * Entry point for a newly fetched ClassroomAnnouncement.
     * Sends to Gemini to extract dates. If a date is found, creates a Calendar event.
     * If no date, marks as AI_PROCESSED with no calendar event.
     */
    public void processAnnouncement(ClassroomAnnouncement announcement) {
        if (announcement.getProcessingState() == MessageProcessingState.AI_PROCESSED) {
            log.info("Announcement is already processed.");
            return;
        }

        log.info("Starting Classroom Announcement Pipeline...");

        ClassroomReasoningResponse reasoningResult;
        try {
            reasoningResult = geminiProvider.evaluateAnnouncement(announcement);
        } catch (Exception e) {
            log.warn("Gemini failed to evaluate Announcement: {}", e.getMessage());
            if (announcement.getId() != null) {
                markAnnouncementProcessed(announcement);
            }
            return;
        }

        // EVALUATE IN MEMORY BEFORE PERSISTING
        if (reasoningResult.getExtractedDate() == null || reasoningResult.getExtractedDate().isBlank()) {
            log.info("Announcement has no extractable date. Discarding immediately without persisting.");
            if (announcement.getId() != null) {
                announcementRepository.delete(announcement);
            }
            return;
        }

        // ACTIONABLE - Save to database now if it's new
        if (announcement.getId() == null) {
            announcement = announcementRepository.save(announcement);
        }

        try {
            commitAnnouncementTransaction(announcement, reasoningResult);
            // Sync calendar outside transaction if we have a date
            syncAnnouncementCalendarEvent(announcement);
        } catch (Exception e) {
            log.error("Failed to commit Announcement pipeline for {}: {}", announcement.getId(), e);
            markAnnouncementProcessed(announcement);
        }
    }

    @Transactional
    protected void commitAnnouncementTransaction(ClassroomAnnouncement announcement, ClassroomReasoningResponse result) {
        // Parse extractedDate if AI found one
        try {
            LocalDateTime extractedDate = LocalDateTime.parse(result.getExtractedDate(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            announcement.setExtractedEventDate(extractedDate);

            // Create a Calendar Event for this announcement date
            User user = announcement.getUser();
            String businessKey = "CLASSROOM_ANNOUNCEMENT_" + announcement.getId();

            Event event = eventRepository.findByTimelineGroupId(businessKey).stream()
                    .findFirst()
                    .orElse(new Event());

            event.setUser(user);
            event.setTitle("[" + announcement.getCourse().getName() + "] " +
                    (result.getTopic() != null ? result.getTopic() : "Classroom Announcement"));
            event.setCompanyOrSource(announcement.getCourse().getName());
            event.setCategory(EventCategory.CLASSROOM_ANNOUNCEMENT);
            event.setEventTime(extractedDate);
            event.setTimelineGroupId(businessKey);
            if (event.getCalendarSyncStatus() == null) {
                event.setCalendarSyncStatus("PENDING");
            }
            eventRepository.save(event);

            // ALSO create a Task so it shows on Dashboard
            Task task = taskRepository.findByBusinessKey(businessKey).orElse(new Task());
            task.setUser(user);
            task.setTitle("[" + announcement.getCourse().getName() + "] " + 
                    (result.getTopic() != null ? result.getTopic() : "Announcement"));
            task.setSource("GOOGLE_CLASSROOM");
            task.setDueDate(extractedDate);
            task.setBusinessKey(businessKey);
            if (task.getId() == null) {
                task.setCompleted(false);
            }
            taskRepository.save(task);

            log.info("Announcement {} has extractedDate {}, creating Calendar event and Task.", announcement.getId(), extractedDate);
            
            announcement.setProcessingState(MessageProcessingState.AI_PROCESSED);
            announcementRepository.save(announcement);
        } catch (Exception e) {
            log.warn("Failed to parse extractedDate '{}' for Announcement {}: {}. Deleting from database.",
                    result.getExtractedDate(), announcement.getId(), e.getMessage());
            announcementRepository.delete(announcement);
        }
    }

    private void syncAnnouncementCalendarEvent(ClassroomAnnouncement announcement) {
        if (announcement.getExtractedEventDate() == null) return;

        String businessKey = "CLASSROOM_ANNOUNCEMENT_" + announcement.getId();
        eventRepository.findByTimelineGroupId(businessKey).stream().findFirst().ifPresent(event -> {
            User user = announcement.getUser();

            CalendarEventDTO gcalDto = CalendarEventDTO.builder()
                    .eventId(event.getId().toString())
                    .title(event.getTitle())
                    .description("Course: " + event.getCompanyOrSource()
                            + "\n\nAnnouncement: " + announcement.getText())
                    .location("")
                    .companyName(event.getCompanyOrSource())
                    .category(EventCategory.CLASSROOM_ANNOUNCEMENT)
                    .startTime(event.getEventTime())
                    .endTime(event.getEventTime().plusHours(1))
                    .requiresReminders(true)
                    .build();

            try {
                String gCalId;
                if (announcement.getGoogleCalendarEventId() != null) {
                    gCalId = googleCalendarSyncService.updateEvent(user, announcement.getGoogleCalendarEventId(), gcalDto);
                } else {
                    gCalId = googleCalendarSyncService.syncEvent(user, gcalDto);
                }
                announcement.setGoogleCalendarEventId(gCalId);
                announcementRepository.save(announcement);
                eventEngine.updateCalendarSyncStatus(event.getId(), gCalId, "SYNCED", null);
                log.info("Announcement Calendar event synced: {}", gCalId);
            } catch (Exception e) {
                log.warn("Calendar sync failed for Announcement {}: {}", announcement.getId(), e.getMessage());
                eventEngine.updateCalendarSyncStatus(event.getId(), null, "FAILED", e);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markAnnouncementProcessed(ClassroomAnnouncement announcement) {
        log.warn("Announcement {} pipeline failed. Deleting from database to discard.", announcement.getId());
        announcementRepository.delete(announcement);
    }

    // =========================================================================
    // SHARED UTILITIES
    // =========================================================================

    /**
     * Maps the AI-classified type string to the correct EventCategory enum.
     * Falls back to CLASSROOM_ASSIGNMENT if unknown.
     */
    private EventCategory resolveEventCategory(String type) {
        if (type == null) return EventCategory.CLASSROOM_ASSIGNMENT;
        return switch (type.toUpperCase()) {
            case "QUIZ"         -> EventCategory.CLASSROOM_QUIZ;
            case "EXAM"         -> EventCategory.CLASSROOM_EXAM;
            case "LAB"          -> EventCategory.CLASSROOM_LAB;
            case "PROJECT"      -> EventCategory.CLASSROOM_PROJECT;
            case "TUTORIAL"     -> EventCategory.CLASSROOM_TUTORIAL;
            case "PRACTICAL"    -> EventCategory.CLASSROOM_PRACTICAL;
            case "MATERIAL"     -> EventCategory.CLASSROOM_MATERIAL;
            case "NOTES"        -> EventCategory.CLASSROOM_NOTES;
            case "ANNOUNCEMENT" -> EventCategory.CLASSROOM_ANNOUNCEMENT;
            default             -> EventCategory.CLASSROOM_ASSIGNMENT;
        };
    }

    private void saveAIProcessingLog(ClassroomCourseWork courseWork, ClassroomReasoningResponse result) {
        try {
            Optional<ClassroomAIProcessingLog> existing = aiProcessingLogRepository.findByCourseWorkId(courseWork.getId());
            if (existing.isPresent()) {
                ClassroomAIProcessingLog logEntry = existing.get();
                logEntry.setExtractedJson(objectMapper.writeValueAsString(result));
                logEntry.setAiSummary(result.getSummary());
                aiProcessingLogRepository.save(logEntry);
            } else {
                ClassroomAIProcessingLog logEntry = ClassroomAIProcessingLog.builder()
                        .courseWork(courseWork)
                        .extractedJson(objectMapper.writeValueAsString(result))
                        .aiSummary(result.getSummary())
                        .build();
                aiProcessingLogRepository.save(logEntry);
            }
        } catch (Exception e) {
            log.warn("Could not save AI log for CourseWork: {}", courseWork.getId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleCourseWorkFailure(ClassroomCourseWork courseWork, Exception ex) {
        log.warn("Pipeline failure for CourseWork {}: {}", courseWork.getId(), ex.getMessage());

        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        boolean isTemporary = message.contains("429") || message.contains("timeout") || message.contains("503");

        if (isTemporary) {
            courseWork.setProcessingState(MessageProcessingState.FAILED);
            courseWork.setRetryCount(courseWork.getRetryCount() + 1);
            long backoffMinutes = (long) Math.pow(2, courseWork.getRetryCount());
            courseWork.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));
            courseWorkRepository.save(courseWork);
        } else {
            log.warn("CourseWork {} failed permanently. Deleting to keep database clean.", courseWork.getId());
            courseWorkRepository.delete(courseWork);
        }
    }
}
