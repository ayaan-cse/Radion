package com.radion.service.pipeline.placement;

import com.radion.service.pipeline.placement.timeline.CompanyMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyMatcherTest {

    private CompanyMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new CompanyMatcher();
    }

    @Test
    void testCompanyNormalization() {
        assertEquals("google", matcher.normalize("Google India Pvt. Ltd."));
        assertEquals("tata consultancy", matcher.normalize("Tata Consultancy Services Limited"));
        assertEquals("microsoft", matcher.normalize("Microsoft Corporation"));
        assertEquals("infosys", matcher.normalize("Infosys Technologies Ltd"));
    }

    @Test
    void testFuzzySimilarityMatching() {
        assertTrue(matcher.isMatch("Google India Pvt Ltd", "Google", 0.75), "Should match normalized Google variants");
        assertTrue(matcher.isMatch("Tata Consultancy Services", "TCS", 0.75), "Should match TCS acronym/substring");
        assertFalse(matcher.isMatch("Google", "Microsoft", 0.75), "Different companies must not match");
    }
}
