package com.radion.service.dev.dto;

import com.radion.service.pipeline.models.BusinessCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DryRunJourneyReasoningResponse {

    private SummaryStats summary;

    @Builder.Default
    private List<EmailReasoningEvaluationResult> results = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryStats {
        private int totalEmails;
        private int impactfulCount;
        private int ignoredCount;
        private int uncertainCount;
        private int errorCount;
        private int totalCommandsPredicted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailReasoningEvaluationResult {
        private UUID messageId;
        private String sender;
        private String subject;
        private String receivedAt;
        private boolean hasJourneyImpact;
        private boolean uncertainty;
        private boolean error;
        private String emailSummary;
        private String uncertaintyReason;
        private String errorMessage;
        private long processingDurationMs;

        @Builder.Default
        private List<BusinessCommand> commands = new ArrayList<>();
    }
}
