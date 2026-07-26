package com.radion.service.pipeline.placement.trust;

import com.radion.domain.models.Message;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class KeywordTrustEvaluator implements TrustEvaluator {

    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "placement cell", "training and placement", "tpo", "campus recruitment",
            "on-campus", "shortlisted", "interview schedule", "offer letter",
            "selection process", "assessment link", "online test", "coding round"
    );

    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "paid internship", "course fee", "registration fee", "pay rs", "pay inr",
            "discount", "coupon", "limited time offer", "buy now", "unsubscribe",
            "masterclass fee", "webinar fee", "resume writing service", "paid certification",
            "training fee", "100% placement guarantee course", "sale starts", "special offer"
    );

    @Override
    public int evaluate(Message message, int currentScore) {
        String content = (
                (message.getSender() != null ? message.getSender() + " " : "") +
                (message.getTitle() != null ? message.getTitle() + " " : "") +
                (message.getSnippet() != null ? message.getSnippet() : "")
        ).toLowerCase();

        int score = currentScore;

        for (String neg : NEGATIVE_KEYWORDS) {
            if (content.contains(neg)) {
                score -= 60;
                break;
            }
        }

        for (String pos : POSITIVE_KEYWORDS) {
            if (content.contains(pos)) {
                score += 15;
                break;
            }
        }

        return score;
    }
}
