package com.radion.service.pipeline.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.service.pipeline.models.AIExtractionResult;
import com.radion.service.pipeline.models.NormalizedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIIntelligenceEngine {

    private final LLMProvider llmProvider;
    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    public AIExtractionResult extractInformation(NormalizedMessage message) {
        String prompt = promptManager.buildPrompt(message);
        
        try {
            // 1. Call the configured LLM provider
            String rawResponse = llmProvider.generateText(prompt);
            
            // 2. Sanitize output (LLMs sometimes ignore instructions and wrap in markdown)
            String cleanJson = sanitizeJsonResponse(rawResponse);
            
            // 3. Parse into DTO
            AIExtractionResult result = objectMapper.readValue(cleanJson, AIExtractionResult.class);
            log.info("AI Extraction successful. Classification: {}, Confidence: {}", 
                     result.getClassification(), result.getConfidenceScore());
            
            return result;
            
        } catch (Exception e) {
            log.error("AI Extraction failed for message ID: {}. Gracefully falling back to IGNORE.", message.getExternalId(), e);
            // Graceful fallback: Return an IGNORE classification so the pipeline doesn't crash
            AIExtractionResult fallback = new AIExtractionResult();
            fallback.setClassification("IGNORE");
            fallback.setConfidenceScore(0.0);
            return fallback;
        }
    }

    private String sanitizeJsonResponse(String raw) {
        if (raw == null) return "{}";
        String clean = raw.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }
}