package com.radion.service.pipeline.placement;

import com.radion.domain.enums.MessageClassification;
import com.radion.domain.models.Message;
import com.radion.service.pipeline.placement.classification.EmailClassificationEngine;
import com.radion.service.pipeline.placement.trust.DomainTrustEvaluator;
import com.radion.service.pipeline.placement.trust.KeywordTrustEvaluator;
import com.radion.service.pipeline.placement.trust.TrustScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrustAndClassificationTest {

    private TrustScoringEngine trustScoringEngine;
    private EmailClassificationEngine classificationEngine;

    @BeforeEach
    void setUp() {
        trustScoringEngine = new TrustScoringEngine(List.of(
                new DomainTrustEvaluator(),
                new KeywordTrustEvaluator()
        ));
        classificationEngine = new EmailClassificationEngine();
    }

    @Test
    void testCollegeTpoTrustAndClassification() {
        Message msg = Message.builder()
                .sender("tpo@iitb.ac.in")
                .title("Campus Recruitment: Google On-Campus Drive")
                .snippet("Please find the registration link for Google 2026 placement drive.")
                .build();

        int score = trustScoringEngine.calculateTrustScore(msg);
        msg.setTrustScore(score);
        assertEquals(100, score, "College TPO email should receive 100 trust score");

        MessageClassification classification = classificationEngine.classify(msg);
        assertEquals(MessageClassification.PLACEMENT, classification);
        assertTrue(classificationEngine.isEligibleForExtraction(classification, score));
    }

    @Test
    void testAtsDomainTrustScore() {
        Message msg = Message.builder()
                .sender("no-reply@greenhouse.io")
                .title("Interview Invitation: Software Engineer at Stripe")
                .snippet("You are invited to an online coding round.")
                .build();

        int score = trustScoringEngine.calculateTrustScore(msg);
        msg.setTrustScore(score);
        assertTrue(score >= 90, "Known ATS should receive trust score >= 90");

        MessageClassification classification = classificationEngine.classify(msg);
        assertEquals(MessageClassification.INTERVIEW, classification);
        assertTrue(classificationEngine.isEligibleForExtraction(classification, score));
    }

    @Test
    void testSpamPaidInternshipDetection() {
        Message msg = Message.builder()
                .sender("recruiter@random-company.com")
                .title("Great Opportunity: Paid internship program")
                .snippet("Pay Rs 999 registration fee for guaranteed placement internship.")
                .build();

        int score = trustScoringEngine.calculateTrustScore(msg);
        msg.setTrustScore(score);

        MessageClassification classification = classificationEngine.classify(msg);
        assertEquals(MessageClassification.SPAM, classification, "Should be detected as SPAM due to paid internship/fee keywords");
        assertFalse(classificationEngine.isEligibleForExtraction(classification, score), "Spam must never be eligible for AI extraction");
    }

    @Test
    void testMarketingCouponDetection() {
        Message msg = Message.builder()
                .sender("promotions@edtech-platform.com")
                .title("Limited time offer: 50% coupon on masterclass")
                .snippet("Use discount coupon SAVE50 to buy now. Unsubscribe anytime.")
                .build();

        int score = trustScoringEngine.calculateTrustScore(msg);
        msg.setTrustScore(score);

        MessageClassification classification = classificationEngine.classify(msg);
        assertEquals(MessageClassification.MARKETING, classification);
        assertFalse(classificationEngine.isEligibleForExtraction(classification, score), "Marketing must never create tasks or timelines");
    }
}
