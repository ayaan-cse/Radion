package com.radion.dto;

import com.radion.domain.enums.EventCategory;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class EventDTO {
    private String id;
    private String time;
    private String title;
    private String source;
    private EventCategory category;
}