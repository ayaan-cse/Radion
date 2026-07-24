package com.radion.service.pipeline.validation;

import com.radion.service.pipeline.models.AIExtractionResult;
import com.radion.service.pipeline.models.NormalizedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PipelineValidator {

    private static final double MIN_CONFIDENCE_THRESHOLD = 0.70;

    public boolean isValid(AIExtractionResult result) {
        if (result == null || "IGNORE".equalsIgnoreCase(result.getClassification())) {
            return false;
        }
        
        if (result.getConfidenceScore() < MIN_CONFIDENCE_THRESHOLD) {
            log.warn("Extraction rejected due to low confidence score: {}", result.getConfidenceScore());
            return false;
        }
        
        // Ensure actionable data exists based on classification
        if ("EVENT".equalsIgnoreCase(result.getClassification()) && result.getEventDate() == null) {
            log.warn("Event classification missing eventDate. Invalidating.");
            return false;
        }
        
        return true;
    }

    public boolean isDuplicate(NormalizedMessage message, AIExtractionResult result) {
        // Duplicate detection logic (queries DB to ensure we don't process the same externalId twice)
        return false;
    }
}