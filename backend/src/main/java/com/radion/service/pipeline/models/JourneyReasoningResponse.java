package com.radion.service.pipeline.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JourneyReasoningResponse {
    
    private String emailSummary;
    private boolean hasJourneyImpact;
    private boolean uncertainty;
    private String uncertaintyReason;
    
    private boolean error;
    private String errorMessage;
    
    @Builder.Default
    private List<BusinessCommand> commands = new ArrayList<>();
}
