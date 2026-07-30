package com.radion.service.engine;

import com.radion.domain.enums.BusinessCommandType;
import com.radion.domain.enums.EventCategory;
import com.radion.domain.enums.TimelineStage;
import com.radion.domain.models.*;
import com.radion.repository.CompanyTimelineRepository;
import com.radion.repository.EventRepository;
import com.radion.repository.TaskRepository;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.engine.dto.CommandExecutionReport;
import com.radion.service.engine.dto.PendingCalendarSync;
import com.radion.service.pipeline.models.BusinessCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessCommandLocalPersister {

    private final CompanyTimelineRepository timelineRepository;
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;

    @Transactional(rollbackFor = Exception.class)
    public PendingCalendarSync persistCommandWorldState(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun, CommandExecutionReport report) {
        switch (command.getCommandType()) {
            case REGISTER_OPPORTUNITY:
                executeCreateOpportunity(user, sourceMessage, command, isDryRun, report);
                return null;
            case ADVANCE_OPPORTUNITY_STAGE:
                executeUpdateOpportunity(user, sourceMessage, command, isDryRun, report);
                return null;
            case ASSIGN_ACTION_ITEM:
                return executeCreateTask(user, sourceMessage, command, isDryRun, report);
            case COMPLETE_ACTION_ITEM:
                executeUpdateTask(user, sourceMessage, command, isDryRun, report);
                return null;
            case ANNOUNCE_EVENT:
            case SCHEDULE_INTERVIEW:
            case SCHEDULE_ASSESSMENT:
                return executeCreateOrUpdateEvent(user, sourceMessage, command, isDryRun, report);
            default:
                report.setValid(false);
                report.getValidationErrors().add("Unsupported command type: " + command.getCommandType());
                report.setErrorMessage("Unsupported command type");
                log.warn("Unsupported command type: {}", command.getCommandType());
                return null;
        }
    }

    private void executeCreateOpportunity(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun, CommandExecutionReport report) {
        String company = resolveCompanyName(command, sourceMessage);
        if (company == null || company.isBlank()) {
            report.setValid(false);
            report.getValidationErrors().add("Missing required field: companyName (or title/sender)");
            report.setErrorMessage("Cannot create Opportunity without Company Name");
            return;
        }

        Optional<CompanyTimeline> existingOpt = timelineRepository.findByUserIdAndCompanyNameIgnoreCase(user.getId(), company);
        if (existingOpt.isPresent()) {
            CompanyTimeline existing = existingOpt.get();
            report.setExecutionPlan("CREATE_OPPORTUNITY: Opportunity [" + company + "] already exists. Transitioning to UPDATE_OPPORTUNITY plan.");
            report.setExecutionResult("Skipped duplicate creation; existing Opportunity found (ID: " + existing.getId() + ")");
            log.info("Duplicate opportunity prevented for company [{}]. Existing ID: {}", company, existing.getId());
            return;
        }

        TimelineStage stage = parseTimelineStage(command.getStage());
        report.setExecutionPlan("CREATE_OPPORTUNITY: Register new Opportunity [" + company + "] at stage [" + stage + "] with Role [" + command.getRole() + "]");

        if (!isDryRun) {
            CompanyTimeline timeline = CompanyTimeline.builder()
                    .user(user)
                    .companyName(company)
                    .currentStage(stage != null ? stage : TimelineStage.REGISTRATION)
                    .role(command.getRole())
                    .salary(command.getCtc())
                    .deadline(command.getDueDate())
                    .registrationLink(command.getMeetingLinkOrUrl())
                    .lastUpdated(LocalDateTime.now())
                    .latestMessage(sourceMessage)
                    .actionRequired(true)
                    .priority("HIGH")
                    .build();
            CompanyTimeline saved = timelineRepository.save(timeline);
            if (saved != null) timeline = saved;
            report.setExecuted(true);
            report.setExecutionResult("Successfully created Opportunity ID: " + timeline.getId());
            log.info("Persisted new Opportunity [{}] (ID: {}) for user {}", company, timeline.getId(), user.getId());
        } else {
            report.setExecuted(true);
            report.setExecutionResult("Dry-Run: Validated & Ready to create Opportunity [" + company + "]");
        }
    }

    private void executeUpdateOpportunity(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun, CommandExecutionReport report) {
        String company = resolveCompanyName(command, sourceMessage);
        if (company == null || company.isBlank()) {
            report.setValid(false);
            report.getValidationErrors().add("Missing required field: companyName (or title/sender)");
            report.setErrorMessage("Cannot update Opportunity without Company Name");
            return;
        }

        Optional<CompanyTimeline> existingOpt = timelineRepository.findByUserIdAndCompanyNameIgnoreCase(user.getId(), company);
        TimelineStage stage = parseTimelineStage(command.getStage());

        if (existingOpt.isPresent()) {
            CompanyTimeline timeline = existingOpt.get();
            report.setExecutionPlan("UPDATE_OPPORTUNITY: Advance Opportunity [" + company + "] from stage [" + timeline.getCurrentStage() + "] to [" + stage + "]");

            if (!isDryRun) {
                if (stage != null && stage != TimelineStage.OTHER) {
                    timeline.setCurrentStage(stage);
                }
                if (command.getRole() != null && !command.getRole().isBlank()) {
                    timeline.setRole(command.getRole());
                }
                if (command.getCtc() != null && !command.getCtc().isBlank()) {
                    timeline.setSalary(command.getCtc());
                }
                if (command.getDueDate() != null && !command.getDueDate().isBlank()) {
                    timeline.setDeadline(command.getDueDate());
                }
                if (command.getMeetingLinkOrUrl() != null && !command.getMeetingLinkOrUrl().isBlank()) {
                    timeline.setRegistrationLink(command.getMeetingLinkOrUrl());
                }
                timeline.setLatestMessage(sourceMessage);
                timeline.setLastUpdated(LocalDateTime.now());
                CompanyTimeline saved = timelineRepository.save(timeline);
                if (saved != null) timeline = saved;
                report.setExecuted(true);
                report.setExecutionResult("Successfully updated Opportunity ID: " + timeline.getId());
                log.info("Updated Opportunity [{}] (ID: {}) to stage [{}] for user {}", company, timeline.getId(), timeline.getCurrentStage(), user.getId());
            } else {
                report.setExecuted(true);
                report.setExecutionResult("Dry-Run: Validated & Ready to update Opportunity [" + company + "]");
            }
        } else {
            report.setExecutionPlan("UPDATE_OPPORTUNITY: Referenced Opportunity [" + company + "] not found. Auto-creating at stage [" + stage + "]");
            if (!isDryRun) {
                CompanyTimeline timeline = CompanyTimeline.builder()
                        .user(user)
                        .companyName(company)
                        .currentStage(stage != null ? stage : TimelineStage.REGISTRATION)
                        .role(command.getRole())
                        .salary(command.getCtc())
                        .deadline(command.getDueDate())
                        .registrationLink(command.getMeetingLinkOrUrl())
                        .lastUpdated(LocalDateTime.now())
                        .latestMessage(sourceMessage)
                        .actionRequired(true)
                        .priority("HIGH")
                        .build();
                CompanyTimeline saved = timelineRepository.save(timeline);
                if (saved != null) timeline = saved;
                report.setExecuted(true);
                report.setExecutionResult("Successfully auto-created Opportunity ID: " + timeline.getId());
                log.info("Auto-created Opportunity [{}] (ID: {}) during update command for user {}", company, timeline.getId(), user.getId());
            } else {
                report.setExecuted(true);
                report.setExecutionResult("Dry-Run: Validated & Ready to auto-create Opportunity [" + company + "]");
            }
        }
    }

    private PendingCalendarSync executeCreateTask(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun, CommandExecutionReport report) {
        String title = resolveTaskTitle(command);
        if (title == null || title.isBlank()) {
            report.setValid(false);
            report.getValidationErrors().add("Missing required field: title or description for Task");
            report.setErrorMessage("Cannot assign Task without Title or Description");
            return null;
        }

        boolean exists = taskRepository.existsByUserIdAndTitleIgnoreCase(user.getId(), title);
        if (exists) {
            report.setExecutionPlan("CREATE_TASK: Task [" + title + "] already exists.");
            report.setExecutionResult("Skipped duplicate task creation");
            log.info("Duplicate task prevented for title [{}] for user {}", title, user.getId());
            return null;
        }

        LocalDateTime dueDate = parseDateTime(command.getDueDate());
        report.setExecutionPlan("CREATE_TASK: Create Action Item [" + title + "] due on [" + dueDate + "]");

        if (!isDryRun) {
            Task task = Task.builder()
                    .user(user)
                    .title(title)
                    .source(command.getCompanyName() != null ? command.getCompanyName() : (sourceMessage != null && sourceMessage.getSender() != null ? sourceMessage.getSender() : "AI Reasoning"))
                    .dueDate(dueDate)
                    .isCompleted(false)
                    .businessKey("TASK-" + UUID.randomUUID().toString().substring(0, 8))
                    .build();
            Task saved = taskRepository.save(task);
            if (saved != null) task = saved;
            report.setExecuted(true);
            report.setExecutionResult("Successfully created Task ID: " + task.getId());
            log.info("Persisted Action Item [{}] (ID: {}) for user {}", title, task.getId(), user.getId());

            Event deadlineEvent = Event.builder()
                    .user(user)
                    .sourceMessage(sourceMessage)
                    .title("[Deadline] " + title)
                    .companyOrSource(task.getSource())
                    .category(EventCategory.DEADLINE)
                    .eventTime(dueDate)
                    .isUserModified(false)
                    .calendarSyncStatus("PENDING")
                    .build();
            deadlineEvent = eventRepository.save(deadlineEvent);

            CalendarEventDTO calendarDTO = buildCalendarDTO(deadlineEvent, command, sourceMessage, task.getSource(), EventCategory.DEADLINE, dueDate);
            calendarDTO.setRegistration(true);

            return new PendingCalendarSync(deadlineEvent, calendarDTO, false);
        } else {
            report.setExecuted(true);
            report.setExecutionResult("Dry-Run: Validated & Ready to create Task [" + title + "]");
            report.setCalendarSyncResult("DRY_RUN: Would sync Deadline Event [" + title + "] to Google Calendar");
            return null;
        }
    }

    private void executeUpdateTask(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun, CommandExecutionReport report) {
        String title = resolveTaskTitle(command);
        if (title == null || title.isBlank()) {
            report.setValid(false);
            report.getValidationErrors().add("Missing required field: title or description for Task completion");
            report.setErrorMessage("Cannot complete Task without Title or Description");
            return;
        }

        Optional<Task> existingOpt = taskRepository.findByUserIdAndTitleIgnoreCase(user.getId(), title).stream().findFirst();
        if (existingOpt.isPresent()) {
            Task task = existingOpt.get();
            report.setExecutionPlan("UPDATE_TASK: Mark Task [" + task.getTitle() + "] as COMPLETED");
            if (!isDryRun) {
                task.setCompleted(true);
                Task saved = taskRepository.save(task);
                if (saved != null) task = saved;
                report.setExecuted(true);
                report.setExecutionResult("Successfully marked Task ID " + task.getId() + " as completed");
                log.info("Completed Task [{}] (ID: {}) for user {}", task.getTitle(), task.getId(), user.getId());
            } else {
                report.setExecuted(true);
                report.setExecutionResult("Dry-Run: Validated & Ready to complete Task [" + task.getTitle() + "]");
            }
        } else {
            report.setExecutionPlan("UPDATE_TASK: Referenced Task [" + title + "] not found. Auto-creating as COMPLETED task.");
            if (!isDryRun) {
                Task task = Task.builder()
                        .user(user)
                        .title(title)
                        .source(command.getCompanyName() != null ? command.getCompanyName() : (sourceMessage != null && sourceMessage.getSender() != null ? sourceMessage.getSender() : "AI Reasoning"))
                        .dueDate(parseDateTime(command.getDueDate()))
                        .isCompleted(true)
                        .businessKey("TASK-" + UUID.randomUUID().toString().substring(0, 8))
                        .build();
                Task saved = taskRepository.save(task);
                if (saved != null) task = saved;
                report.setExecuted(true);
                report.setExecutionResult("Successfully created completed Task ID: " + task.getId());
                log.info("Auto-created completed Task [{}] (ID: {}) for user {}", title, task.getId(), user.getId());
            } else {
                report.setExecuted(true);
                report.setExecutionResult("Dry-Run: Validated & Ready to auto-create completed Task [" + title + "]");
            }
        }
    }

    private PendingCalendarSync executeCreateOrUpdateEvent(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun, CommandExecutionReport report) {
        String title = command.getTitle() != null && !command.getTitle().isBlank() ? command.getTitle() : "Scheduled Event";
        String company = command.getCompanyName() != null ? command.getCompanyName() : (sourceMessage != null && sourceMessage.getSender() != null ? sourceMessage.getSender() : "General");
        String timeStr = command.getScheduledTime() != null ? command.getScheduledTime() : command.getDueDate();
        
        if (timeStr == null || timeStr.isBlank()) {
            report.setValid(false);
            report.getValidationErrors().add("Missing required field: scheduledTime or dueDate for Event");
            report.setErrorMessage("Cannot schedule Event without Time/Date");
            return null;
        }

        LocalDateTime eventTime = parseDateTime(timeStr);
        Optional<Event> existingOpt = eventRepository.findByUserIdAndCompanyOrSourceAndEventTime(user.getId(), company, eventTime);

        if (existingOpt.isPresent()) {
            Event event = existingOpt.get();
            report.setExecutionPlan("UPDATE_EVENT: Update existing Event [" + event.getTitle() + "] at [" + eventTime + "] to new title [" + title + "]");
            if (!isDryRun) {
                event.setTitle(title);
                event.setCalendarSyncStatus("PENDING");
                Event saved = eventRepository.save(event);
                if (saved != null) event = saved;
                report.setExecuted(true);
                report.setExecutionResult("Successfully updated Event ID: " + event.getId());
                log.info("Updated Event [{}] (ID: {}) at [{}] for user {}", title, event.getId(), eventTime, user.getId());

                CalendarEventDTO calendarDTO = buildCalendarDTO(event, command, sourceMessage, company, event.getCategory() != null ? event.getCategory() : EventCategory.MEETING, eventTime);
                return new PendingCalendarSync(event, calendarDTO, event.getGoogleCalendarEventId() != null);
            } else {
                report.setExecuted(true);
                report.setExecutionResult("Dry-Run: Validated & Ready to update Event [" + title + "]");
                report.setCalendarSyncResult("DRY_RUN: Would sync Event [" + title + "] to Google Calendar");
                report.setCalendarEventId("DRY-RUN-CAL-ID");
                return null;
            }
        }

        EventCategory category = EventCategory.MEETING;
        if (command.getCommandType() == BusinessCommandType.SCHEDULE_INTERVIEW) {
            category = EventCategory.INTERVIEW;
        } else if (command.getCommandType() == BusinessCommandType.SCHEDULE_ASSESSMENT) {
            category = EventCategory.DEADLINE;
            if (!isDryRun && company != null && !company.isBlank() && !"General".equals(company)) {
                try {
                    executeUpdateOpportunity(user, sourceMessage, BusinessCommand.builder()
                            .commandType(BusinessCommandType.ADVANCE_OPPORTUNITY_STAGE)
                            .companyName(company)
                            .stage("ASSESSMENT")
                            .build(), false, new CommandExecutionReport());
                } catch (Exception e) {
                    log.warn("Secondary opportunity stage advance failed during assessment scheduling for company [{}]: {}", company, e.getMessage());
                }
            }
        }

        report.setExecutionPlan("CREATE_EVENT: Schedule new " + category + " Event [" + title + "] for [" + company + "] at [" + eventTime + "]");

        if (!isDryRun) {
            Event event = Event.builder()
                    .user(user)
                    .sourceMessage(sourceMessage)
                    .companyOrSource(company)
                    .eventTime(eventTime)
                    .title(title)
                    .category(category)
                    .isUserModified(false)
                    .calendarSyncStatus("PENDING")
                    .build();
            Event saved = eventRepository.save(event);
            if (saved != null) event = saved;
            report.setExecuted(true);
            report.setExecutionResult("Successfully created Event ID: " + event.getId());
            log.info("Scheduled new Event [{}] (ID: {}) at [{}] for user {}", title, event.getId(), eventTime, user.getId());

            CalendarEventDTO calendarDTO = buildCalendarDTO(event, command, sourceMessage, company, category, eventTime);
            return new PendingCalendarSync(event, calendarDTO, false);
        } else {
            report.setExecuted(true);
            report.setExecutionResult("Dry-Run: Validated & Ready to create Event [" + title + "]");
            report.setCalendarSyncResult("DRY_RUN: Would sync Event [" + title + "] to Google Calendar");
            report.setCalendarEventId("DRY-RUN-CAL-ID");
            return null;
        }
    }

    private CalendarEventDTO buildCalendarDTO(Event event, BusinessCommand command, Message sourceMessage, String company, EventCategory category, LocalDateTime eventTime) {
        String desc = command.getDescription() != null ? command.getDescription() : (sourceMessage != null && sourceMessage.getSnippet() != null ? sourceMessage.getSnippet() : "");
        String loc = command.getMeetingLinkOrUrl() != null ? command.getMeetingLinkOrUrl() : "Online / Remote";
        return CalendarEventDTO.builder()
                .eventId(event.getId() != null ? event.getId().toString() : UUID.randomUUID().toString())
                .title(event.getTitle() + (company != null && !company.isBlank() ? " - " + company : ""))
                .description(desc)
                .location(loc)
                .companyName(company)
                .category(category)
                .startTime(eventTime)
                .endTime(eventTime.plusHours(1))
                .isRegistration(false)
                .requiresReminders(true)
                .build();
    }

    private String resolveCompanyName(BusinessCommand command, Message sourceMessage) {
        if (command.getCompanyName() != null && !command.getCompanyName().isBlank()) {
            return command.getCompanyName().trim();
        }
        if (command.getTitle() != null && !command.getTitle().isBlank() && !command.getTitle().toLowerCase().contains("action")) {
            return command.getTitle().trim();
        }
        if (sourceMessage != null && sourceMessage.getSender() != null && !sourceMessage.getSender().isBlank()) {
            return sourceMessage.getSender().trim();
        }
        return null;
    }

    private String resolveTaskTitle(BusinessCommand command) {
        if (command.getTitle() != null && !command.getTitle().isBlank()) {
            return command.getTitle().trim();
        }
        if (command.getDescription() != null && !command.getDescription().isBlank()) {
            String desc = command.getDescription().trim();
            return desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
        }
        if (command.getCompanyName() != null && !command.getCompanyName().isBlank()) {
            return "Action Required: " + command.getCompanyName().trim();
        }
        return null;
    }

    private TimelineStage parseTimelineStage(String stageStr) {
        if (stageStr == null || stageStr.isBlank() || "null".equalsIgnoreCase(stageStr)) return TimelineStage.OTHER;
        String upper = stageStr.trim().toUpperCase();
        if (upper.contains("REGIST")) return TimelineStage.REGISTRATION;
        if (upper.contains("ASSESS") || upper.contains("TEST") || upper.contains("EXAM") || upper.contains("CODING")) return TimelineStage.ASSESSMENT;
        if (upper.contains("TECH")) return TimelineStage.TECHNICAL;
        if (upper.contains("HR") || upper.contains("MANAG")) return TimelineStage.HR;
        if (upper.contains("OFFER") || upper.contains("SELECT") || upper.contains("JOIN")) return TimelineStage.OFFER;
        if (upper.contains("REJECT") || upper.contains("REGRET")) return TimelineStage.REJECTED;
        return TimelineStage.OTHER;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) {
            return LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0);
        }
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr.trim(), DateTimeFormatter.ISO_DATE_TIME);
            }
            LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_DATE);
            return date.atTime(LocalTime.of(17, 0));
        } catch (DateTimeParseException e) {
            return LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0);
        }
    }
}
