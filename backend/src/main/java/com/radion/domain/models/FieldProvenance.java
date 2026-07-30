package com.radion.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldProvenance {
    private String source;
    private Double confidence;
    private LocalDateTime extractedAt;
    private String messageId;
}
