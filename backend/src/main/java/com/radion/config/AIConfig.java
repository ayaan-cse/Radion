package com.radion.config;

import com.radion.service.pipeline.ai.LLMProvider;
import com.radion.service.pipeline.ai.providers.GeminiProvider;
import com.radion.service.pipeline.ai.providers.OpenAIProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AIConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "radion.ai.provider", havingValue = "openai")
    public LLMProvider openAIProvider(RestTemplate restTemplate) {
        return new OpenAIProvider(restTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "radion.ai.provider", havingValue = "gemini", matchIfMissing = true)
    public GeminiProvider geminiProvider(RestTemplate restTemplate) {
        return new GeminiProvider(restTemplate);
    }
}