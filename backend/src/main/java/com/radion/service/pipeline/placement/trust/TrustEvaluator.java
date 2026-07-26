package com.radion.service.pipeline.placement.trust;

import com.radion.domain.models.Message;

public interface TrustEvaluator {
    /**
     * Evaluates the message and returns the updated trust score.
     *
     * @param message The message to evaluate.
     * @param currentScore The accumulated trust score so far.
     * @return The updated score after applying this evaluator's rules.
     */
    int evaluate(Message message, int currentScore);
}
