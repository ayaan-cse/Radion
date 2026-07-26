package com.radion.service.pipeline.placement.trust;

import com.radion.domain.models.Message;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(1)
public class DomainTrustEvaluator implements TrustEvaluator {

    private static final Set<String> KNOWN_ATS_DOMAINS = Set.of(
            "greenhouse.io", "lever.co", "workday.com", "myworkdayjobs.com",
            "hackerrank.com", "mettl.com", "superset.com", "naukri.com",
            "instahyre.com", "smartrecruiters.com", "icims.com", "jobvite.com",
            "ashbyhq.com", "unstop.com", "hirepro.in", "cocubes.com", "amcat.com"
    );

    private static final Set<String> FREE_WEBMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "icloud.com", "rediffmail.com", "aol.com", "zoho.com", "protonmail.com"
    );

    private static final Set<String> MARKETING_DOMAINS = Set.of(
            "mailchimp.com", "sendgrid.net", "hubspot.com", "substack.com",
            "mailerlite.com", "constantcontact.com", "klaviyo.com", "brevo.com"
    );

    @Override
    public int evaluate(Message message, int currentScore) {
        String sender = message.getSender();
        if (sender == null || sender.isBlank()) {
            return currentScore;
        }

        String lowerSender = sender.toLowerCase();
        String domain = extractDomain(lowerSender);

        // 1. Marketing / Bulk Mailers -> 20
        if (MARKETING_DOMAINS.contains(domain) || domain.startsWith("marketing.") || domain.contains("newsletter") || domain.startsWith("promo.")) {
            return 20;
        }

        // 2. College TPO / University domain -> 100
        if (domain.endsWith(".ac.in") || domain.endsWith(".edu") || domain.endsWith(".edu.in") ||
            lowerSender.contains("tpo@") || lowerSender.contains("placement@") || lowerSender.contains("campus@")) {
            return 100;
        }

        // 3. Known ATS / Testing Platforms -> 90
        if (KNOWN_ATS_DOMAINS.contains(domain) || KNOWN_ATS_DOMAINS.stream().anyMatch(domain::endsWith)) {
            return Math.max(currentScore, 90);
        }

        // 4. Free Webmail (Unknown Recruiter / General) -> 70
        if (FREE_WEBMAIL_DOMAINS.contains(domain)) {
            return Math.max(currentScore, 70);
        }

        // 5. Official Company Domain -> 95
        return Math.max(currentScore, 95);
    }

    private String extractDomain(String emailOrSender) {
        int atIndex = emailOrSender.lastIndexOf('@');
        if (atIndex != -1 && atIndex < emailOrSender.length() - 1) {
            String domainPart = emailOrSender.substring(atIndex + 1);
            int bracketIndex = domainPart.indexOf('>');
            if (bracketIndex != -1) {
                domainPart = domainPart.substring(0, bracketIndex);
            }
            return domainPart.trim();
        }
        return emailOrSender.trim();
    }
}
