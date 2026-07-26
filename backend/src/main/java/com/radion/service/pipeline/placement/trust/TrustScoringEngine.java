package com.radion.service.pipeline.placement.trust;

import com.radion.domain.models.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoringEngine {

    private final List<TrustEvaluator> evaluators;

    /**
     * Calculates a trust score (0 to 100) for a given message by running it through all registered evaluators.
     *
     * @param message The email or notification message to evaluate.
     * @return Clamped trust score between 0 and 100.
     */
    public int calculateTrustScore(Message message) {
        int score = 50; // Default baseline trust score

        for (TrustEvaluator evaluator : evaluators) {
            score = evaluator.evaluate(message, score);
        }

        // Clamp between 0 and 100
        int finalScore = Math.max(0, Math.min(100, score));
        log.debug("Trust score calculated for message {}: {} (Evaluator count: {})", 
                  message.getId(), finalScore, evaluators.size());
        return finalScore;
    }
}
