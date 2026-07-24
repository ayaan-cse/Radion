package com.radion.service.pipeline.models;

import com.radion.domain.enums.Platform;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class NormalizedMessage {
    private String externalId;
    private Platform platform;
    private String sender;
    private String subject;
    private String body;
    private LocalDateTime receivedAt;
}