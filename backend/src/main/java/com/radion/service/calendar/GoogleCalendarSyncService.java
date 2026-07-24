package com.radion.service.calendar;

import com.radion.domain.models.User;
import com.radion.service.calendar.dto.CalendarEventDTO;

public interface GoogleCalendarSyncService {
    /**
     * Syncs a Radion event to the user's connected Google Calendar.
     * Returns the external Google Calendar Event ID.
     */
    String syncEvent(User user, CalendarEventDTO eventDTO);
    
    /**
     * Updates an existing Google Calendar event if the timeline/schedule changes.
     */
    void updateEvent(User user, String googleCalendarEventId, CalendarEventDTO eventDTO);
}