package com.radion.service.pipeline.ai;

public interface LLMProvider {
    /**
     * Sends a prompt to the LLM and returns the raw string response (expected to be JSON).
     */
    String generateText(String prompt);
}