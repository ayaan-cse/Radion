package com.radion.dto;

import com.radion.domain.enums.EventCategory;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UpcomingEventDTO {
    private String id;
    private String day;
    private String month;
    private String company;
    private String title;
    private String time;
    private EventCategory category;
}