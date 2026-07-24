package com.radion.service.calendar.dto;

import com.radion.domain.enums.EventCategory;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class CalendarEventDTO {
    private String eventId; // Internal Radion DB ID
    private String title;
    private String description;
    private String location;
    private String companyName;
    private EventCategory category;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private boolean isRegistration;
    private boolean requiresReminders;
}