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
import com.radion.repository.UserRepository;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.calendar.dto.CalendarEventDTO;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarSyncServiceImpl implements GoogleCalendarSyncService {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final UserRepository userRepository;

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String syncEvent(User user, CalendarEventDTO dto) {
        return doSyncEvent(user, dto, null);
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String updateEvent(User user, String existingGoogleEventId, CalendarEventDTO dto) {
        return doSyncEvent(user, dto, existingGoogleEventId);
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void deleteEvent(User user, String existingGoogleEventId) {
        if (existingGoogleEventId == null || existingGoogleEventId.isBlank()) return;
        
        if (user.getGoogleAccessToken() == null) {
            log.warn("Skipping Google Calendar delete for event {}: No valid token for user {}", existingGoogleEventId, user.getId());
            return;
        }

        if (!googleOAuthService.refreshUserAccessToken(user)) {
            log.warn("Failed to refresh token for user {}", user.getId());
            return;
        }
        userRepository.save(user);

        try {
            Calendar calendarService = buildCalendarClient(user.getGoogleAccessToken());
            calendarService.events().delete("primary", existingGoogleEventId).execute();
            log.info("Successfully deleted Google Calendar event: {}", existingGoogleEventId);
        } catch (Exception e) {
            log.warn("Failed or already deleted event {} from Google Calendar: {}", existingGoogleEventId, e.getMessage());
        }
    }

    private String doSyncEvent(User user, CalendarEventDTO dto, String existingGoogleEventId) {
        if (user.getGoogleAccessToken() == null && user.getGoogleRefreshToken() == null) {
            log.info("Skipping Google Calendar sync: User {} has not connected Google Calendar via Dashboard login", user.getId());
            return null;
        }

        try {
            if (!googleOAuthService.refreshUserAccessToken(user)) {
                throw new RuntimeException("Failed to refresh Google token for User " + user.getId());
            }
        } catch (GoogleOAuthServiceImpl.InvalidGrantException e) {
            // Permanent OAuth failure — propagate so EventEngine marks REAUTH_REQUIRED, not FAILED
            log.error("Calendar sync aborted for user {}: {}", user.getId(), e.getMessage());
            userRepository.save(user); // persist cleared tokens
            throw e;
        }
        userRepository.save(user);

        try {
            Calendar calendarService = buildCalendarClient(user.getGoogleAccessToken());

            boolean isUpdate = existingGoogleEventId != null;
            Event googleEvent;

            if (isUpdate) {
                try {
                    googleEvent = calendarService.events().get("primary", existingGoogleEventId).execute();
                } catch (Exception e) {
                    log.warn("Existing event {} not found in Google Calendar, falling back to insert", existingGoogleEventId);
                    isUpdate = false;
                    googleEvent = new Event();
                }
            } else {
                googleEvent = new Event();
            }

            // 1. Basic Info
            googleEvent.setSummary(dto.getTitle());
            googleEvent.setDescription(dto.getDescription());
            googleEvent.setLocation(dto.getLocation());

            // 2. Time & Timezone Handling
            ZoneId zone = ZoneId.of("Asia/Kolkata");
            // Google API requires strict RFC 3339 format with seconds included (HH:mm:ss)
            // Java's toString() omits seconds if they are zero. We must explicitly format it.
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            
            String startRfc = dto.getStartTime().atZone(zone).format(formatter);
            String endRfc = dto.getEndTime().atZone(zone).format(formatter);

            DateTime startDateTime = new DateTime(startRfc);
            DateTime endDateTime = new DateTime(endRfc);
            
            googleEvent.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(zone.getId()));
            googleEvent.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(zone.getId()));

            // 3. Color Coding
            googleEvent.setColorId(getColorIdForCategory(dto.getCategory(), dto.isRegistration()));

            // 4. Dynamic Multi-Stage Reminders
            if (dto.isRequiresReminders()) {
                Event.Reminders reminders = new Event.Reminders().setUseDefault(false);
                reminders.setOverrides(calculateDynamicReminders(dto.getStartTime(), dto.isRegistration()));
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

        } catch (GoogleOAuthServiceImpl.InvalidGrantException e) {
            throw e; // Already logged, propagate as-is
        } catch (Exception e) {
            log.error("Google Calendar API failure for user: {}", user.getId(), e);
            throw new RuntimeException("Calendar sync failed", e);
        }
    }

    private List<EventReminder> calculateDynamicReminders(LocalDateTime eventStartTime, boolean isRegistration) {
        long minutesUntilEvent = ChronoUnit.MINUTES.between(LocalDateTime.now(), eventStartTime);
        if (minutesUntilEvent <= 0) {
            return Collections.singletonList(new EventReminder().setMethod("popup").setMinutes(15));
        }

        List<Integer> candidateMinutes = isRegistration
                ? Arrays.asList(30 * 24 * 60, 14 * 24 * 60, 7 * 24 * 60, 5 * 24 * 60, 3 * 24 * 60, 2 * 24 * 60, 24 * 60, 12 * 60, 6 * 60, 2 * 60, 60, 30)
                : Arrays.asList(7 * 24 * 60, 3 * 24 * 60, 24 * 60, 12 * 60, 6 * 60, 2 * 60, 60, 30, 15);

        List<Integer> validMinutes = candidateMinutes.stream()
                .filter(m -> m < minutesUntilEvent)
                .collect(Collectors.toList());

        if (validMinutes.isEmpty()) {
            int halfTime = (int) (minutesUntilEvent / 2);
            return Collections.singletonList(new EventReminder().setMethod("popup").setMinutes(Math.max(15, halfTime)));
        }

        List<Integer> selectedMinutes = new ArrayList<>();
        if (validMinutes.size() <= 5) {
            selectedMinutes.addAll(validMinutes);
        } else {
            int size = validMinutes.size();
            selectedMinutes.add(validMinutes.get(0));
            selectedMinutes.add(validMinutes.get(size / 4));
            selectedMinutes.add(validMinutes.get(size / 2));
            selectedMinutes.add(validMinutes.get(3 * size / 4));
            selectedMinutes.add(validMinutes.get(size - 1));
        }

        return selectedMinutes.stream()
                .distinct()
                .map(m -> new EventReminder().setMethod("popup").setMinutes(m))
                .collect(Collectors.toList());
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
            case DEADLINE   -> "11"; // Tomato (Red)
            case MEETING    -> "3";  // Grape (Purple)
            case TASK       -> "9";  // Blueberry

            // All Classroom events → Basil Green ("10")
            case CLASSROOM_ASSIGNMENT,
                 CLASSROOM_QUIZ,
                 CLASSROOM_EXAM,
                 CLASSROOM_LAB,
                 CLASSROOM_PROJECT,
                 CLASSROOM_TUTORIAL,
                 CLASSROOM_PRACTICAL,
                 CLASSROOM_ANNOUNCEMENT,
                 CLASSROOM_MATERIAL,
                 CLASSROOM_NOTES -> "10"; // Basil (Green)

            default -> "9"; // Blueberry fallback
        };
    }
}