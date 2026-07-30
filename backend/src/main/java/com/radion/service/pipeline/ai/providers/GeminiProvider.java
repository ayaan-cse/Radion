package com.radion.service.pipeline.ai.providers;

import com.radion.service.pipeline.ai.LLMProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GeminiProvider implements LLMProvider {

    private final RestTemplate restTemplate;

    @Value("${radion.ai.gemini.api-key}")
    private String apiKey;

    @Value("${radion.ai.gemini.model:gemini-flash-latest}")
    private String model;

    @Value("${radion.ai.gemini.max-retries:3}")
    private int maxRetries;

    @Override
    public String generateText(String prompt) {
        return generateTextWithSystemInstruction(prompt, null);
    }

    public String generateTextWithSystemInstruction(String prompt, String systemInstruction) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        requestBody.put("generationConfig", Map.of(
                "temperature", 0.0, // Strictly deterministic for classification and extraction
                "responseMimeType", "application/json" // Enforce JSON
        ));
        
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            requestBody.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        }

        long backoffMs = 1000L;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            } catch (Exception e) {
                boolean isRetryable = isRetryableException(e);
                if (isRetryable && attempt < maxRetries) {
                    log.warn("Gemini API call failed on attempt {}/{}. Retrying in {}ms. Error: {}", attempt, maxRetries, backoffMs, e.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during exponential backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    log.error("Gemini API call failed completely after {} attempts. Full error:", attempt, e);
                    throw new RuntimeException("LLM Provider unavailable: " + e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException("LLM Provider unavailable after retries");
    }

    private boolean isRetryableException(Exception e) {
        if (e instanceof HttpStatusCodeException hsce) {
            int status = hsce.getStatusCode().value();
            return status == 429 || (status >= 500 && status <= 599);
        }
        return e instanceof ResourceAccessException;
    }
}