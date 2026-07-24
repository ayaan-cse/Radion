package com.radion.service.calendar.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.repository.ConnectedServiceRepository;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarSyncServiceImpl implements GoogleCalendarSyncService {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final ConnectedServiceRepository connectedServiceRepository;

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String syncEvent(User user, CalendarEventDTO dto) {
        return doSyncEvent(user, dto, null);
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void updateEvent(User user, String googleCalendarEventId, CalendarEventDTO dto) {
        doSyncEvent(user, dto, googleCalendarEventId);
    }

    private String doSyncEvent(User user, CalendarEventDTO dto, String existingGoogleEventId) {
        ConnectedService googleConnection = connectedServiceRepository
                .findByUserId(user.getId()).stream()
                .filter(c -> c.getPlatform() == Platform.GMAIL) // Gmail connection holds the Google token
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No Google connection found for user"));

        if (!googleOAuthService.refreshAccessToken(googleConnection)) {
            throw new RuntimeException("Failed to refresh Google token");
        }

        Calendar calendarService = buildCalendarClient(googleConnection.getAccessToken());
        
        try {
            Event googleEvent;
            boolean isUpdate = existingGoogleEventId != null;

            if (isUpdate) {
                googleEvent = calendarService.events().get("primary", existingGoogleEventId).execute();
                // Protection: Do not overwrite if user manually removed our tracking property
                if (googleEvent.getExtendedProperties() == null || 
                    !googleEvent.getExtendedProperties().getPrivate().containsKey("radion_managed")) {
                    log.info("Event {} was modified by user. Skipping update.", existingGoogleEventId);
                    return existingGoogleEventId;
                }
            } else {
                googleEvent = new Event();
            }

            // 1. Basic Info
            googleEvent.setSummary(dto.getTitle());
            googleEvent.setDescription(dto.getDescription());
            googleEvent.setLocation(dto.getLocation());

            // 2. Time & Timezone Handling
            ZoneId zone = ZoneId.systemDefault();
            DateTime startDateTime = new DateTime(dto.getStartTime().atZone(zone).toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(dto.getEndTime().atZone(zone).toInstant().toEpochMilli());
            
            googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(zone.getId()));
            googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(zone.getId()));

            // 3. Color Coding
            googleEvent.setColorId(getColorIdForCategory(dto.getCategory(), dto.isRegistration()));

            // 4. Reminders (Especially for Registrations)
            if (dto.isRequiresReminders()) {
                Event.Reminders reminders = new Event.Reminders().setUseDefault(false);
                if (dto.isRegistration()) {
                    // Remind 24 hours and 2 hours before deadline
                    reminders.setOverrides(Arrays.asList(
                            new EventReminder().setMethod("popup").setMinutes(24 * 60),
                            new EventReminder().setMethod("popup").setMinutes(2 * 60)
                    ));
                } else {
                    // Standard 30 min reminder
                    reminders.setOverrides(Arrays.asList(
                            new EventReminder().setMethod("popup").setMinutes(30)
                    ));
                }
                googleEvent.setReminders(reminders);
            }

            // 5. Timeline Linking via Extended Properties
            Event.ExtendedProperties extendedProps = new Event.ExtendedProperties();
            Map<String, String> privateProps = new HashMap<>();
            privateProps.put("radion_managed", "true");
            privateProps.put("radion_event_id", dto.getEventId());
            privateProps.put("radion_company", dto.getCompanyName());
            extendedProps.setPrivate(privateProps);
            googleEvent.setExtendedProperties(extendedProps);

            // 6. Execute API Call
            if (isUpdate) {
                Event updatedEvent = calendarService.events().update("primary", existingGoogleEventId, googleEvent).execute();
                log.info("Successfully updated Google Calendar event: {}", updatedEvent.getId());
                return updatedEvent.getId();
            } else {
                Event createdEvent = calendarService.events().insert("primary", googleEvent).execute();
                log.info("Successfully created Google Calendar event: {}", createdEvent.getId());
                return createdEvent.getId();
            }

        } catch (Exception e) {
            log.error("Google Calendar API failure for user: {}", user.getId(), e);
            throw new RuntimeException("Calendar sync failed", e);
        }
    }

    private Calendar buildCalendarClient(String accessToken) {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Calendar.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Radion Dashboard")
                .build();
    }

    private String getColorIdForCategory(com.radion.domain.enums.EventCategory category, boolean isRegistration) {
        if (isRegistration) return "11"; // Tomato (Red) for Deadlines/Registrations
        if (category == null) return "9"; // Default Blueberry
        
        return switch (category) {
            case INTERVIEW -> "9"; // Blueberry (Blue)
            case TASK -> "10";     // Basil (Green)
            case DEADLINE -> "11"; // Tomato (Red)
            case MEETING -> "3";   // Grape (Purple)
            default -> "9";
        };
    }
}