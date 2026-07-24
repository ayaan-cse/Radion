package com.radion.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class NotificationDTO {
    private String id;
    private String title;
    private String content;
    private String timestamp;
    private boolean isRead;
}