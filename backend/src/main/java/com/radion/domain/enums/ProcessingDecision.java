package com.radion.domain.enums;

/**
 * Represents the actionable decision made by the AI Decision Engine for an incoming message.
 */
public enum ProcessingDecision {
    /**
     * Continue to Gemini extraction.
     */
    PROCESS,
    /**
     * Stop processing immediately (spam, marketing, promotions, webinars, social, newsletters).
     */
    IGNORE,
    /**
     * Stop processing for now and route to manual review queue (low confidence or uncertain trust).
     */
    REVIEW,
    /**
     * Classification failed due to API exhaustion, timeouts, or unhandled exceptions.
     */
    ERROR
}
