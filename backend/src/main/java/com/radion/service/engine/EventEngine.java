package com.radion.service.engine;

import com.radion.domain.models.Event;
import com.radion.domain.models.Message;
import com.radion.domain.models.User;
import com.radion.repository.EventRepository;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.pipeline.models.AIExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventEngine {

    private final EventRepository eventRepository;
    private final GoogleCalendarSyncService calendarSyncService;

    @Transactional
    public Event createOrUpdateEvent(User user, Message sourceMessage, AIExtractionResult extraction) {
        LocalDateTime eventDateTime = extraction.getEventDate().atTime(
                extraction.getEventTime() != null ? extraction.getEventTime() : java.time.LocalTime.of(23, 59)
        );

        boolean isRegistration = extraction.getSubject().toLowerCase().contains("registration") || 
                                 extraction.getSubject().toLowerCase().contains("apply");

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
                    .build();
        }

        eventToSave = eventRepository.save(eventToSave);

        // 3. Prepare Calendar DTO
        LocalDateTime startTime = isRegistration ? sourceMessage.getReceivedAt() : eventDateTime;
        LocalDateTime endTime = isRegistration ? eventDateTime : eventDateTime.plusHours(1);

        CalendarEventDTO calendarDTO = CalendarEventDTO.builder()
                .eventId(eventToSave.getId().toString())
                .title(eventToSave.getTitle() + (extraction.getCompanyName() != null ? " - " + extraction.getCompanyName() : ""))
                .description(buildDescription(extraction, sourceMessage))
                .location(extraction.getLocation())
                .companyName(extraction.getCompanyName())
                .category(extraction.getCategory())
                .startTime(startTime)
                .endTime(endTime)
                .isRegistration(isRegistration)
                .requiresReminders(true)
                .build();

        // 4. Sync to Google Calendar
        try {
            if (eventToSave.getGoogleCalendarEventId() != null) {
                calendarSyncService.updateEvent(user, eventToSave.getGoogleCalendarEventId(), calendarDTO);
            } else {
                String gCalId = calendarSyncService.syncEvent(user, calendarDTO);
                eventToSave.setGoogleCalendarEventId(gCalId);
            }
            eventRepository.save(eventToSave);
        } catch (Exception e) {
            log.error("Failed to sync event {} to Google Calendar", eventToSave.getId(), e);
            // We swallow the exception here so the Radion DB transaction still commits.
            // A background job can retry failed calendar syncs later.
        }

        return eventToSave;
    }

    private String buildDescription(AIExtractionResult extraction, Message sourceMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(extraction.getSummary()).append("\n\n");
        if (extraction.getRole() != null) sb.append("Role: ").append(extraction.getRole()).append("\n");
        if (extraction.getCtc() != null) sb.append("CTC: ").append(extraction.getCtc()).append("\n");
        if (extraction.getRegistrationLink() != null) sb.append("Link: ").append(extraction.getRegistrationLink()).append("\n");
        sb.append("\nSource: ").append(sourceMessage.getPlatform());
        return sb.toString();
    }
}