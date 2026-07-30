package com.radion.service.pipeline.automation;

import com.radion.domain.models.Event;
import com.radion.domain.models.Message;
import com.radion.domain.models.User;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.engine.EventEngine;
import com.radion.service.engine.TaskEngine;
import com.radion.service.notification.NotificationChannel;
import com.radion.service.notification.NotificationEngine;
import com.radion.service.pipeline.models.AIExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEngine {

    private final EventEngine eventEngine;
    private final TaskEngine taskEngine;
    private final NotificationEngine notificationEngine;
    private final GoogleCalendarSyncService googleCalendarSyncService;

    public enum ActionType { CALENDAR_EVENT, TASK, NOTIFICATION_ONLY, INFO_ONLY }

    public ActionType determineAction(AIExtractionResult extraction) {
        if (extraction.getEventDate() != null && extraction.getCategory() != com.radion.domain.enums.EventCategory.TASK) {
            return ActionType.CALENDAR_EVENT;
        } else if (extraction.isActionRequired() || extraction.getCategory() == com.radion.domain.enums.EventCategory.TASK) {
            return ActionType.TASK;
        } else if ("HIGH".equalsIgnoreCase(extraction.getPriority())) {
            return ActionType.NOTIFICATION_ONLY;
        }
        return ActionType.INFO_ONLY;
    }

    public void execute(User user, Message sourceMessage, AIExtractionResult extraction) {
        ActionType action = determineAction(extraction);
        log.info("Automation Engine determined action: {} for message: {}", action, sourceMessage.getId());

        switch (action) {
            case CALENDAR_EVENT -> {
                // 1. Save to Radion DB (Handles Timeline Linking & Duplicates)
                Event event = eventEngine.createOrUpdateEvent(user, sourceMessage, extraction);
                
                // 2. Sync to Google Calendar
                CalendarEventDTO gcalDto = CalendarEventDTO.builder()
                        .eventId(event.getId().toString())
                        .title(event.getTitle() + " - " + event.getCompanyOrSource())
                        .description(extraction.getSummary() + "\n\nSource: " + sourceMessage.getPlatform())
                        .location(extraction.getLocation())
                        .companyName(event.getCompanyOrSource())
                        .category(event.getCategory())
                        .startTime(event.getEventTime())
                        .endTime(event.getEventTime().plusHours(1)) // Default 1 hr duration
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
                    log.warn("Automation Engine Google Calendar sync failed for event {}: {}", event.getId(), e.getMessage());
                    eventEngine.updateCalendarSyncStatus(event.getId(), null, "FAILED", e);
                }

                // 3. Notify User
                notificationEngine.dispatch(user, 
                        "New Event Scheduled", 
                        event.getTitle() + " has been added to your calendar.", 
                        List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
            }
            case TASK -> {
                // 1. Save Task (Handles Registration spanning to deadline)
                taskEngine.createTask(user, extraction, sourceMessage.getPlatform().name());
                
                // 2. Notify User
                notificationEngine.dispatch(user, 
                        "New Task Assigned", 
                        extraction.getSubject() + " requires your action.", 
                        List.of(NotificationChannel.IN_APP));
            }
            case NOTIFICATION_ONLY -> {
                notificationEngine.dispatch(user, 
                        "High Priority Update", 
                        extraction.getSummary(), 
                        List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
            }
            case INFO_ONLY -> log.info("Information only. Logged for dashboard context but no active alerts triggered.");
        }
    }
}