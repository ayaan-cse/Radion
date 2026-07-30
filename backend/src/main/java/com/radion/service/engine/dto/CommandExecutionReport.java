package com.radion.service.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.radion.service.pipeline.models.BusinessCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed execution report produced by BusinessCommandExecutor for each evaluated Business Command.
 * Captures validation results, proposed execution plan, actual persistence result, and any errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandExecutionReport {
    private BusinessCommand rawCommand;
    private boolean valid;
    
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    
    private String executionPlan;
    private boolean executed;
    private String executionResult;
    private String errorMessage;
    private String calendarSyncResult;
    private String calendarEventId;
}
