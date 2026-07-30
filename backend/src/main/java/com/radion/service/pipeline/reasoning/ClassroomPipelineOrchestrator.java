package com.radion.service.pipeline.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.EventCategory;
import com.radion.domain.enums.MessageProcessingState;
import com.radion.domain.models.ClassroomAIProcessingLog;
import com.radion.domain.models.ClassroomCourseWork;
import com.radion.domain.models.Event;
import com.radion.domain.models.Task;
import com.radion.domain.models.User;
import com.radion.repository.ClassroomAIProcessingLogRepository;
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
import java.util.Optional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomPipelineOrchestrator {

    private final ClassroomCourseWorkRepository courseWorkRepository;
    private final ClassroomAIProcessingLogRepository aiProcessingLogRepository;
    private final GeminiClassroomReasoningProvider geminiProvider;
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final GoogleCalendarSyncService googleCalendarSyncService;
    private final EventEngine eventEngine;
    private final ObjectMapper objectMapper;

    /**
     * Entry point for a newly fetched ClassroomCourseWork item.
     * Splitting transactions so API calls aren't wrapped in @Transactional.
     */
    public void processCourseWork(ClassroomCourseWork courseWork) {
        if (courseWork.getProcessingState() == MessageProcessingState.AI_PROCESSED) {
            log.info("CourseWork {} is already processed.", courseWork.getId());
            return;
        }

        log.info("Starting Classroom Pipeline for CourseWork: {}", courseWork.getId());

        ClassroomReasoningResponse reasoningResult = null;
        
        if (courseWork.getDueDate() != null && courseWork.getTitle() != null) {
            log.info("CourseWork {} already has due date and title, bypassing Gemini", courseWork.getId());
            reasoningResult = ClassroomReasoningResponse.builder()
                .isActionRequired(true)
                .topic(courseWork.getTitle())
                .summary(courseWork.getDescription() != null && courseWork.getDescription().length() > 0 ? 
                    courseWork.getDescription().substring(0, Math.min(courseWork.getDescription().length(), 150)) + "..." : 
                    "Classroom Assignment")
                .build();
        } else {
            try {
                reasoningResult = geminiProvider.evaluateCourseWork(courseWork);
            } catch (Exception e) {
                handlePipelineFailure(courseWork, e);
                return;
            }
        }

        try {
            commitReasoningTransaction(courseWork, reasoningResult);
            
            // Sync calendar OUTSIDE of the transaction
            syncCalendarEvent(courseWork);
        } catch (Exception e) {
            log.error("Failed to save and translate reasoning for CourseWork: {}", courseWork.getId(), e);
            handlePipelineFailure(courseWork, e);
        }
    }

    @Transactional
    protected void commitReasoningTransaction(ClassroomCourseWork courseWork, ClassroomReasoningResponse result) {
        log.info("Committing Classroom Reasoning for CourseWork: {}", courseWork.getId());

        saveAIProcessingLog(courseWork, result);

        User user = courseWork.getUser();
        String businessKey = "CLASSROOM_COURSEWORK_" + courseWork.getId();

        if (result.isActionRequired() || courseWork.getDueDate() != null) {
            // UPSERT Task
            Task task = taskRepository.findByBusinessKey(businessKey).orElse(new Task());
            task.setUser(user);
            task.setTitle(courseWork.getTitle() + " - " + result.getTopic());
            task.setSource("GOOGLE_CLASSROOM");
            task.setDueDate(courseWork.getDueDate());
            task.setBusinessKey(businessKey);
            
            // Prevent overwriting completed state if already set by user
            if (task.getId() == null) {
                task.setCompleted(false);
            }
            
            taskRepository.save(task);

            // UPSERT Event (We will map this directly so calendar sync picks it up)
            if (courseWork.getDueDate() != null) {
                Event event = eventRepository.findBySourceCourseWorkId(courseWork.getId()).orElse(new Event());
                event.setUser(user);
                event.setSourceCourseWork(courseWork);
                event.setTitle(courseWork.getTitle());
                event.setCompanyOrSource(courseWork.getCourse().getName());
                event.setCategory(EventCategory.CLASSROOM_ASSIGNMENT);
                event.setEventTime(courseWork.getDueDate());
                event.setTimelineGroupId("CLASSROOM_" + courseWork.getCourse().getId());
                
                if (event.getCalendarSyncStatus() == null) {
                    event.setCalendarSyncStatus("PENDING");
                }
                eventRepository.save(event);
            }
        }

        courseWork.setProcessingState(MessageProcessingState.AI_PROCESSED);
        courseWorkRepository.save(courseWork);
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

    private void syncCalendarEvent(ClassroomCourseWork courseWork) {
        Optional<Event> eventOpt = eventRepository.findBySourceCourseWorkId(courseWork.getId());
        if (eventOpt.isEmpty()) return;

        Event event = eventOpt.get();
        User user = courseWork.getUser();

        CalendarEventDTO gcalDto = CalendarEventDTO.builder()
                .eventId(event.getId().toString())
                .title("Classroom: " + event.getTitle())
                .description("Course: " + event.getCompanyOrSource() + "\n\n" + courseWork.getDescription())
                .location("")
                .companyName(event.getCompanyOrSource())
                .category(EventCategory.CLASSROOM_ASSIGNMENT)
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
            
            // Transactional update for status
            eventEngine.updateCalendarSyncStatus(event.getId(), gCalId, "SYNCED", null);
        } catch (Exception e) {
            log.warn("Calendar Sync failed for Classroom Event {}: {}", event.getId(), e.getMessage());
            eventEngine.updateCalendarSyncStatus(event.getId(), null, "FAILED", e);
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handlePipelineFailure(ClassroomCourseWork courseWork, Exception ex) {
        log.warn("Pipeline failure for CourseWork {}: {}", courseWork.getId(), ex.getMessage());
        
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        boolean isTemporary = message.contains("429") || message.contains("timeout") || message.contains("503");

        if (isTemporary) {
            courseWork.setProcessingState(MessageProcessingState.FAILED);
            courseWork.setRetryCount(courseWork.getRetryCount() + 1);
            long backoffMinutes = (long) Math.pow(2, courseWork.getRetryCount());
            courseWork.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));
        } else {
            courseWork.setProcessingState(MessageProcessingState.PERMANENTLY_FAILED);
        }
        
        courseWorkRepository.save(courseWork);
    }
}
