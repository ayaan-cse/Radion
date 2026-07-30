package com.radion.service.pipeline.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.models.Message;
import com.radion.service.pipeline.ai.LLMProvider;
import com.radion.service.pipeline.models.JourneyReasoningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Default implementation of JourneyReasoningAIProvider using the configured LLMProvider (Gemini/OpenAI).
 * Executes Entity-Based Real-World Journey Reasoning with 100% determinism (temperature 0.0) and retry resilience.
 * Emits Business Commands describing what happened in the student's world.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiJourneyReasoningProvider implements JourneyReasoningAIProvider {

    private final LLMProvider llmProvider;
    private final JourneyReasoningPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public JourneyReasoningResponse reasonOverMessage(Message message, String activeContextJson) {
        String prompt = promptBuilder.buildPrompt(message, activeContextJson);

        try {
            log.info("Executing AI Journey Reasoning for message ID: {}", message.getId());
            String rawResponse = llmProvider.generateText(prompt);
            String cleanJson = sanitizeJsonResponse(rawResponse);
            
            JourneyReasoningResponse result = objectMapper.readValue(cleanJson, JourneyReasoningResponse.class);

            if (result.getCommands() == null) {
                result.setCommands(new ArrayList<>());
            }

            log.info("AI Journey Reasoning successful for message ID: {}. HasImpact: {}, Command Count: {}, Uncertainty: {}",
                     message.getId(), result.isHasJourneyImpact(), result.getCommands().size(), result.isUncertainty());
            return result;

        } catch (Exception e) {
            log.warn("AI Journey Reasoning failed for message ID: {}. Returning explicit error response.", message.getId(), e);
            return JourneyReasoningResponse.builder()
                    .emailSummary("AI Reasoning Error: " + e.getMessage())
                    .hasJourneyImpact(false)
                    .uncertainty(true)
                    .uncertaintyReason("API or parsing failure: " + e.getMessage())
                    .error(true)
                    .errorMessage(e.getMessage())
                    .commands(new ArrayList<>())
                    .build();
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
