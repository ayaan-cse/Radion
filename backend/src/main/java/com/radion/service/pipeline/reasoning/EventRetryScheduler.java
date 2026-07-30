package com.radion.service.pipeline.reasoning;

import com.radion.domain.enums.EventCategory;
import com.radion.domain.models.Event;
import com.radion.repository.EventRepository;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.engine.EventEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRetryScheduler {

    private final EventRepository eventRepository;
    private final GoogleCalendarSyncService googleCalendarSyncService;
    private final EventEngine eventEngine;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void retryFailedEvents() {
        List<Event> failedEvents = eventRepository.findFailedEventsForRetry(3, LocalDateTime.now());

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("EventRetryScheduler is running... Found {} events to retry.", failedEvents.size());

        for (Event event : failedEvents) {
            int currentRetry = event.getRetryCount() != null ? event.getRetryCount() : 0;
            log.info("Attempting retry {} for event: {} - '{}'", currentRetry + 1, event.getId(), event.getTitle());

            try {
                CalendarEventDTO gcalDto = buildRetryDto(event);

                String gCalId;
                if (event.getGoogleCalendarEventId() != null) {
                    gCalId = googleCalendarSyncService.updateEvent(event.getUser(), event.getGoogleCalendarEventId(), gcalDto);
                } else {
                    gCalId = googleCalendarSyncService.syncEvent(event.getUser(), gcalDto);
                }

                eventEngine.updateCalendarSyncStatus(event.getId(), gCalId, "SYNCED", null);
                log.info("Retry successful for event: {} -> GCal ID: {}", event.getId(), gCalId);

            } catch (com.radion.service.integration.oauth.GoogleOAuthServiceImpl.InvalidGrantException e) {
                // Permanent OAuth failure — mark REAUTH_REQUIRED, stop retrying
                log.error("Retry stopped for event {}: OAuth token invalid. User must re-login.", event.getId());
                eventEngine.updateCalendarSyncStatus(event.getId(), null, "FAILED", e);

            } catch (Exception e) {
                log.warn("Retry {} failed for event {}: {}", currentRetry + 1, event.getId(), e.getMessage());
                eventEngine.updateCalendarSyncStatus(event.getId(), null, "FAILED", e);
            }
        }
    }

    private CalendarEventDTO buildRetryDto(Event event) {
        String description = "Automatically scheduled event.";

        // Try to pull original context if available
        if (event.getSourceMessage() != null && event.getSourceMessage().getSnippet() != null) {
            description = event.getSourceMessage().getSnippet();
        } else if (event.getSourceCourseWork() != null && event.getSourceCourseWork().getDescription() != null) {
            description = "Course: " + event.getCompanyOrSource() + "\n\n"
                    + event.getSourceCourseWork().getDescription();
        }

        return CalendarEventDTO.builder()
                .eventId(event.getId().toString())
                .title(event.getTitle() + (event.getCompanyOrSource() != null && !event.getCompanyOrSource().isBlank()
                        ? " - " + event.getCompanyOrSource()
                        : ""))
                .description(description)
                .location("Online / Remote")
                .companyName(event.getCompanyOrSource())
                .category(event.getCategory() != null ? event.getCategory() : EventCategory.MEETING)
                .startTime(event.getEventTime())
                .endTime(event.getEventTime().plusHours(1))
                .isRegistration(event.getCategory() == EventCategory.DEADLINE)
                .requiresReminders(true)
                .build();
    }
}
