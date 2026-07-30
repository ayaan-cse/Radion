package com.radion.service.pipeline.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.radion.domain.enums.BusinessCommandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a semantic Business Command emitted by the AI Reasoning Engine
 * describing an event or change that occurred in the student's real-world academic/placement state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessCommand {
    private BusinessCommandType commandType;
    private String companyName;
    private String title;
    private String role;
    private String stage;
    private String ctc;
    private String scheduledTime;
    private String dueDate;
    private String meetingLinkOrUrl;
    private String description;
    private String evidenceQuote;

    // Execution Layer Simulation & Validation Report Fields
    private Boolean valid;
    private java.util.List<String> validationErrors;
    private String executionPlan;
    private String executionResult;
    private String executionError;
    private String calendarSyncResult;
    private String calendarEventId;
}
