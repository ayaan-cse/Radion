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
public class OpenAIProvider implements LLMProvider {

    private final RestTemplate restTemplate;
    
    @Value("${radion.ai.openai.api-key}")
    private String apiKey;

    @Value("${radion.ai.openai.max-retries:3}")
    private int maxRetries;

    @Override
    public String generateText(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini", // Fast, cost-effective, great at JSON
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.0, // Strictly deterministic for classification and extraction
            "response_format", Map.of("type", "json_object") // Enforce JSON
        );

        long backoffMs = 1000L;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            } catch (Exception e) {
                boolean isRetryable = isRetryableException(e);
                if (isRetryable && attempt < maxRetries) {
                    log.warn("OpenAI API call failed on attempt {}/{}. Retrying in {}ms. Error: {}", attempt, maxRetries, backoffMs, e.getMessage());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during exponential backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    log.error("OpenAI API call failed completely after {} attempts. Full error:", attempt, e);
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