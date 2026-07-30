package com.radion.service.engine.dto;

import com.radion.domain.models.Event;
import com.radion.service.calendar.dto.CalendarEventDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingCalendarSync {
    private Event event;
    private CalendarEventDTO calendarDTO;
    private boolean isUpdate;
}
