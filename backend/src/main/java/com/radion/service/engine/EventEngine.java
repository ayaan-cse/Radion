package com.radion.service.engine;

import com.radion.domain.models.Event;
import com.radion.domain.models.Message;
import com.radion.domain.models.User;
import com.radion.repository.EventRepository;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import com.radion.service.pipeline.models.AIExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventEngine {

    private final EventRepository eventRepository;

    @Transactional
    public Event createOrUpdateEvent(User user, Message sourceMessage, AIExtractionResult extraction) {
        LocalDateTime eventDateTime = extraction.getEventDate().atTime(
                extraction.getEventTime() != null ? extraction.getEventTime() : java.time.LocalTime.of(23, 59)
        );

        // 1. Duplicate Prevention & Update Logic
        Optional<Event> existingEvent = eventRepository.findByUserIdAndCompanyOrSourceAndEventTime(
                user.getId(), extraction.getCompanyName(), eventDateTime);
        
        Event eventToSave;
        if (existingEvent.isPresent()) {
            eventToSave = existingEvent.get();
            eventToSave.setTitle(extraction.getSubject());
            log.info("Updating existing event for {}", extraction.getCompanyName());
        } else {
            // 2. Timeline Linking
            String timelineGroupId = extraction.getCompanyName() + "-" + LocalDateTime.now().getYear();
            
            eventToSave = Event.builder()
                    .user(user)
                    .sourceMessage(sourceMessage)
                    .title(extraction.getSubject())
                    .companyOrSource(extraction.getCompanyName())
                    .category(extraction.getCategory())
                    .eventTime(eventDateTime)
                    .timelineGroupId(timelineGroupId)
                    .calendarSyncStatus("PENDING")
                    .build();
        }

        return eventRepository.save(eventToSave);
    }

    @Transactional
    public void updateCalendarSyncStatus(UUID eventId, String gCalId, String status, Exception syncException) {
        eventRepository.findById(eventId).ifPresent(event -> {
            if (gCalId != null) {
                event.setGoogleCalendarEventId(gCalId);
            }

            // InvalidGrantException = permanent, no retry — user must re-login
            if (syncException instanceof GoogleOAuthServiceImpl.InvalidGrantException) {
                log.error("Event {} marked REAUTH_REQUIRED: Google OAuth token is invalid. User must re-login.", eventId);
                event.setCalendarSyncStatus("REAUTH_REQUIRED");
                event.setCalendarSyncError(syncException.getMessage());
                event.setNextRetryAt(null);
                eventRepository.save(event);
                return;
            }

            String error = syncException != null ? syncException.getMessage() : null;
            if (error != null && error.length() > 2000) {
                event.setCalendarSyncError(error.substring(0, 2000) + "...");
            } else {
                event.setCalendarSyncError(error);
            }

            if ("SYNCED".equals(status)) {
                event.setCalendarSyncStatus("SYNCED");
                event.setRetryCount(0);
                event.setNextRetryAt(null);
                log.info("Updated event {} sync status to SYNCED. GCal ID: {}", eventId, gCalId);
            } else if ("FAILED".equals(status)) {
                handleSyncFailure(event, error);
            } else {
                event.setCalendarSyncStatus(status);
            }

            eventRepository.save(event);
        });
    }

    private void handleSyncFailure(Event event, String errorStr) {
        if (errorStr == null) errorStr = "Unknown error";
        String lowerError = errorStr.toLowerCase();
        
        boolean isTemporary = lowerError.contains("429") || 
                              lowerError.contains("500") || 
                              lowerError.contains("502") || 
                              lowerError.contains("503") || 
                              lowerError.contains("504") || 
                              lowerError.contains("timeout") || 
                              lowerError.contains("connection") ||
                              lowerError.contains("refresh") ||
                              lowerError.contains("socket") ||
                              lowerError.contains("no session") ||           // Hibernate LazyLoad outside TX
                              lowerError.contains("session") ||              // Hibernate session closed
                              lowerError.contains("could not initialize proxy") || // LazyInitializationException
                              lowerError.contains("failed to lazily initialize") || // LazyInitializationException
                              lowerError.contains("service unavailable") ||
                              lowerError.contains("network");

        if (isTemporary) {
            int currentRetry = event.getRetryCount() != null ? event.getRetryCount() : 0;
            if (currentRetry >= 2) { // Max 3 attempts total (initial + 2 retries)
                log.error("Event {} sync failed after {} retries. Marking PERMANENTLY_FAILED.", event.getId(), currentRetry);
                event.setCalendarSyncStatus("PERMANENTLY_FAILED");
                event.setNextRetryAt(null);
            } else {
                int[] delaySeconds = {30, 120}; // Retry 1 = 30s, Retry 2 = 120s
                int delay = delaySeconds[currentRetry];
                event.setRetryCount(currentRetry + 1);
                event.setNextRetryAt(LocalDateTime.now().plusSeconds(delay));
                event.setCalendarSyncStatus("FAILED");
                log.warn("Event {} sync failed (Attempt {}). Retrying in {} seconds at {}", 
                         event.getId(), event.getRetryCount(), delay, event.getNextRetryAt());
            }
        } else {
            log.error("Event {} encountered a permanent sync failure. Error: {}", event.getId(), errorStr);
            event.setCalendarSyncStatus("PERMANENTLY_FAILED");
            event.setNextRetryAt(null);
        }
    }
}