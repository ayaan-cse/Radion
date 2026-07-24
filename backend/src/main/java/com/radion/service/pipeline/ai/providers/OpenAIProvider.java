package com.radion.service.pipeline.ai.providers;

import com.radion.service.pipeline.ai.LLMProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class OpenAIProvider implements LLMProvider {

    private final RestTemplate restTemplate;
    
    @Value("${radion.ai.openai.api-key}")
    private String apiKey;

    @Override
    public String generateText(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini", // Fast, cost-effective, great at JSON
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.1, // Low temperature for deterministic JSON output
            "response_format", Map.of("type", "json_object") // Enforce JSON
        );

        try {
            Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(requestBody, headers), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new RuntimeException("LLM Provider unavailable", e);
        }
    }
}