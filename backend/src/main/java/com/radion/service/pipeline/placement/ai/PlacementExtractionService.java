package com.radion.service.pipeline.placement.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.models.Message;
import com.radion.service.pipeline.ai.LLMProvider;
import com.radion.service.pipeline.placement.dto.PlacementExtractionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementExtractionService {

    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper;

    /**
     * Sends the email metadata to Gemini 1.5 Flash and parses the structured JSON response.
     *
     * @param message The trusted placement message to extract from.
     * @return Structured PlacementExtractionDTO.
     */
    public PlacementExtractionDTO extractPlacementData(Message message) {
        String prompt = buildPrompt(message);
        try {
            String rawResponse = llmProvider.generateText(prompt);
            String cleanJson = sanitizeJsonResponse(rawResponse);
            PlacementExtractionDTO result = objectMapper.readValue(cleanJson, PlacementExtractionDTO.class);
            
            if (result.getConfidence() == null) {
                result.setConfidence(0.80);
            }
            log.info("Gemini extraction success for message {}. Company: {}, Role: {}, Stage: {}, Confidence: {}",
                     message.getId(), result.getCompany(), result.getRole(), result.getStage(), result.getConfidence());
            return result;
        } catch (Exception e) {
            log.error("Gemini extraction failed for message {}. Returning fallback DTO.", message.getId(), e);
            return PlacementExtractionDTO.builder()
                    .company(extractFallbackCompany(message))
                    .role("Placement Opportunity")
                    .stage("OTHER")
                    .actionRequired(false)
                    .priority("LOW")
                    .confidence(0.0) // Low confidence triggers manual review
                    .build();
        }
    }

    private String buildPrompt(Message message) {
        return """
            You are an expert AI recruitment assistant for college students. Analyze the following recruitment/placement email and extract exact structured information in strict JSON format.
            
            Sender: %s
            Subject: %s
            Snippet: %s
            
            Required JSON structure:
            {
              "company": "Company name (or null if unknown)",
              "role": "Job title or role (or null if unknown)",
              "employmentType": "Full-Time, Internship, Contract, etc. (or null)",
              "stage": "REGISTRATION, ASSESSMENT, TECHNICAL, HR, OFFER, REJECTED, or OTHER",
              "deadline": "Date string or ISO YYYY-MM-DD (or null)",
              "interviewDate": "Date string (or null)",
              "assessmentDate": "Date string (or null)",
              "location": "Location or Remote (or null)",
              "salary": "CTC / Stipend details (or null)",
              "eligibility": "Eligibility criteria summary (or null)",
              "registrationLink": "URL to apply/register (or null)",
              "actionRequired": true or false,
              "priority": "HIGH, MEDIUM, or LOW",
              "confidence": 0.0 to 1.0 (float representing your confidence in this extraction accuracy)"
            }
            Only output valid JSON with no markdown formatting or commentary.
            """.formatted(
                message.getSender() != null ? message.getSender() : "Unknown",
                message.getTitle() != null ? message.getTitle() : "No Subject",
                message.getSnippet() != null ? message.getSnippet() : "No Content"
        );
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

    private String extractFallbackCompany(Message message) {
        if (message.getTitle() != null && !message.getTitle().isBlank()) {
            return message.getTitle().split("-")[0].trim();
        }
        return "Unknown Company";
    }
}
