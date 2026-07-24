package com.radion.dto;

import com.radion.domain.enums.Platform;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class MessageDTO {
    private String id;
    private Platform platform;
    private String title;
    private String summary;
    private String timestamp;
    private boolean isUnread;
}