package com.radion.service.pipeline.models;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomReasoningResponse {
    private String topic;
    private String priority;
    private List<String> actionItems;
    private String reminderStrategy;
    private boolean isActionRequired;
    private String summary;

    /**
     * AI-classified type: ASSIGNMENT, QUIZ, EXAM, LAB, TUTORIAL, PROJECT,
     * PRACTICAL, MATERIAL, NOTES, ANNOUNCEMENT
     */
    private String type;

    /**
     * ISO-8601 date string extracted from announcement text (e.g. "2026-08-10T14:00:00").
     * Null if no date found or item is not an announcement.
     */
    private String extractedDate;
}
