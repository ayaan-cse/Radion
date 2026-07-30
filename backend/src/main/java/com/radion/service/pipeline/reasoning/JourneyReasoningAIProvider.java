package com.radion.service.pipeline.reasoning;

import com.radion.domain.models.Message;
import com.radion.service.pipeline.models.JourneyReasoningResponse;

/**
 * Interface for AI providers that execute Entity-Based Real-World Journey Reasoning.
 */
public interface JourneyReasoningAIProvider {

    /**
     * Evaluates incoming evidence (message) against the student's active real-world journey context
     * and produces structured Business Commands describing what happened in the student's world.
     *
     * @param message The email or message evidence to reason over.
     * @param activeContextJson JSON string representing active Opportunities, Tasks, and Events.
     * @return JourneyReasoningResponse containing the evaluated business commands and reasoning summary.
     */
    JourneyReasoningResponse reasonOverMessage(Message message, String activeContextJson);
}
