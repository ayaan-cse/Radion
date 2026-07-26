package com.radion.service.pipeline.placement.classification;

import com.radion.domain.enums.MessageClassification;
import com.radion.domain.models.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class EmailClassificationEngine {

    private static final List<String> SPAM_KEYWORDS = List.of(
            "paid internship", "paid certification", "training program requiring payment",
            "course fee", "registration fee", "pay rs", "pay inr", "resume service",
            "resume writing", "training fee", "100% placement guarantee course", "masterclass fee"
    );

    private static final List<String> MARKETING_KEYWORDS = List.of(
            "unsubscribe", "promotional newsletter", "coupon", "discount code",
            "sales campaign", "limited time offer", "buy now", "special offer",
            "weekly newsletter", "marketing newsletter"
    );

    private static final Set<MessageClassification> PLACEMENT_ELIGIBLE_CLASSES = Set.of(
            MessageClassification.PLACEMENT,
            MessageClassification.RECRUITMENT,
            MessageClassification.INTERNSHIP,
            MessageClassification.ASSESSMENT,
            MessageClassification.INTERVIEW,
            MessageClassification.SELECTION,
            MessageClassification.RESULT,
            MessageClassification.DEADLINE
    );

    /**
     * Classifies a message into one of the 13 defined MessageClassification categories.
     * Enforces automatic filtering of SPAM and MARKETING messages.
     *
     * @param message The message to classify.
     * @return The determined MessageClassification.
     */
    public MessageClassification classify(Message message) {
        int trustScore = message.getTrustScore() != null ? message.getTrustScore() : 50;
        String content = (
                (message.getSender() != null ? message.getSender() + " " : "") +
                (message.getTitle() != null ? message.getTitle() + " " : "") +
                (message.getSnippet() != null ? message.getSnippet() : "")
        ).toLowerCase();

        // 1. Spam & Marketing Detection (Priority Override)
        for (String spamWord : SPAM_KEYWORDS) {
            if (content.contains(spamWord)) {
                log.info("Message {} classified as SPAM due to keyword: {}", message.getId(), spamWord);
                return MessageClassification.SPAM;
            }
        }

        for (String mktWord : MARKETING_KEYWORDS) {
            if (content.contains(mktWord)) {
                log.info("Message {} classified as MARKETING due to keyword: {}", message.getId(), mktWord);
                return MessageClassification.MARKETING;
            }
        }

        if (trustScore <= 20) {
            return MessageClassification.MARKETING;
        }

        // 2. Specific Placement Lifecycle Stages
        if (content.contains("last date") || content.contains("deadline") || content.contains("apply by") || content.contains("closes on") || content.contains("due date")) {
            return MessageClassification.DEADLINE;
        }

        if (content.contains("result announced") || content.contains("selected candidates") || content.contains("shortlisted") || content.contains("offer letter") || content.contains("final result")) {
            return MessageClassification.RESULT;
        }

        if (content.contains("interview") || content.contains("technical round") || content.contains("hr round") || content.contains("coding round")) {
            return MessageClassification.INTERVIEW;
        }

        if (content.contains("online test") || content.contains("coding assessment") || content.contains("aptitude") || content.contains("hackerrank") || content.contains("mettl") || content.contains("assessment")) {
            return MessageClassification.ASSESSMENT;
        }

        if (content.contains("selection process") || content.contains("pre-placement talk") || content.contains("ppt") || content.contains("recruitment drive")) {
            return MessageClassification.SELECTION;
        }

        if (content.contains("internship") || content.contains("summer intern") || content.contains("intern drive")) {
            return MessageClassification.INTERNSHIP;
        }

        if (content.contains("placement") || content.contains("on campus drive") || content.contains("tpo")) {
            return MessageClassification.PLACEMENT;
        }

        if (content.contains("hiring") || content.contains("job opening") || content.contains("career") || content.contains("recruitment")) {
            return MessageClassification.RECRUITMENT;
        }

        // 3. Fallbacks
        if (content.contains("newsletter") || content.contains("digest")) {
            return MessageClassification.NEWSLETTER;
        }

        if (trustScore >= 80) {
            return MessageClassification.RECRUITMENT; // Default trusted ATS and TPO emails to recruitment/placement
        }

        return MessageClassification.OTHER;
    }

    /**
     * Determines whether a message should proceed to Gemini AI Extraction.
     * Only trusted placement/recruitment emails are eligible.
     *
     * @param classification The message classification.
     * @param trustScore The message trust score.
     * @return true if eligible for AI extraction, false otherwise.
     */
    public boolean isEligibleForExtraction(MessageClassification classification, int trustScore) {
        if (classification == MessageClassification.SPAM || classification == MessageClassification.MARKETING || classification == MessageClassification.NEWSLETTER) {
            return false;
        }
        return trustScore >= 30 && PLACEMENT_ELIGIBLE_CLASSES.contains(classification);
    }
}
