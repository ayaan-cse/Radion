package com.radion.service.pipeline.models;

import com.radion.domain.enums.Platform;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RawPayload {
    private String externalMessageId;
    private Platform platform;
    private String rawJsonContent; // The raw JSON from Gmail/WhatsApp/Classroom APIs
}