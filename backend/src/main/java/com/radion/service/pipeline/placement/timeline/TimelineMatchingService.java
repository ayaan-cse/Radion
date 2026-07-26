package com.radion.service.pipeline.placement.timeline;

import com.radion.domain.enums.TimelineStage;
import com.radion.domain.models.CompanyTimeline;
import com.radion.domain.models.Message;
import com.radion.domain.models.User;
import com.radion.repository.CompanyTimelineRepository;
import com.radion.service.pipeline.placement.dto.PlacementExtractionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineMatchingService {

    private final CompanyTimelineRepository timelineRepository;
    private final CompanyMatcher companyMatcher;

    @Value("${radion.pipeline.company-similarity-threshold:0.75}")
    private double similarityThreshold;

    private static final List<TimelineStage> FUNNEL_ORDER = List.of(
            TimelineStage.REGISTRATION,
            TimelineStage.ASSESSMENT,
            TimelineStage.TECHNICAL,
            TimelineStage.HR,
            TimelineStage.OFFER
    );

    /**
     * Finds an existing company timeline for the user or creates a new one.
     * Enforces the 1-to-1 company timeline rule using normalized fuzzy similarity matching.
     *
     * @param user The student user.
     * @param message The source message.
     * @param extraction The Gemini extraction DTO.
     * @return The created or updated CompanyTimeline entity.
     */
    @Transactional
    public CompanyTimeline matchAndUpdateTimeline(User user, Message message, PlacementExtractionDTO extraction) {
        String companyName = extraction.getCompany();
        if (companyName == null || companyName.isBlank() || "null".equalsIgnoreCase(companyName)) {
            companyName = "General Placement Opportunity";
        }
        companyName = companyName.trim();

        TimelineStage newStage = parseStage(extraction.getStage());

        // 1. Search existing timelines using normalized fuzzy similarity
        List<CompanyTimeline> userTimelines = timelineRepository.findByUserId(user.getId());
        Optional<CompanyTimeline> matchedTimeline = findMatchingTimeline(userTimelines, companyName);

        CompanyTimeline timeline;
        if (matchedTimeline.isPresent()) {
            timeline = matchedTimeline.get();
            log.info("Matched existing timeline for company '{}' (ID: {}) using fuzzy similarity threshold {}. Updating stage...", 
                     timeline.getCompanyName(), timeline.getId(), similarityThreshold);

            // Advance stage if applicable
            if (shouldAdvanceStage(timeline.getCurrentStage(), newStage)) {
                log.info("Advancing timeline stage for '{}' from {} to {}", 
                         timeline.getCompanyName(), timeline.getCurrentStage(), newStage);
                timeline.setCurrentStage(newStage);
            }

            // Update metadata if new extraction provided richer info
            if (extraction.getRole() != null && !extraction.getRole().isBlank() && !"null".equalsIgnoreCase(extraction.getRole())) {
                timeline.setRole(extraction.getRole());
            }
            if (extraction.getEmploymentType() != null && !extraction.getEmploymentType().isBlank() && !"null".equalsIgnoreCase(extraction.getEmploymentType())) {
                timeline.setEmploymentType(extraction.getEmploymentType());
            }
            if (extraction.getSalary() != null && !extraction.getSalary().isBlank() && !"null".equalsIgnoreCase(extraction.getSalary())) {
                timeline.setSalary(extraction.getSalary());
            }
            if (extraction.getLocation() != null && !extraction.getLocation().isBlank() && !"null".equalsIgnoreCase(extraction.getLocation())) {
                timeline.setLocation(extraction.getLocation());
            }
            if (extraction.getEligibility() != null && !extraction.getEligibility().isBlank() && !"null".equalsIgnoreCase(extraction.getEligibility())) {
                timeline.setEligibility(extraction.getEligibility());
            }
            if (extraction.getRegistrationLink() != null && !extraction.getRegistrationLink().isBlank() && !"null".equalsIgnoreCase(extraction.getRegistrationLink())) {
                timeline.setRegistrationLink(extraction.getRegistrationLink());
            }
        } else {
            log.info("Creating new 1-to-1 timeline for company '{}' for user {}", companyName, user.getId());
            timeline = CompanyTimeline.builder()
                    .user(user)
                    .companyName(companyName)
                    .role(extraction.getRole() != null && !"null".equalsIgnoreCase(extraction.getRole()) ? extraction.getRole() : "Candidate")
                    .employmentType(extraction.getEmploymentType() != null && !"null".equalsIgnoreCase(extraction.getEmploymentType()) ? extraction.getEmploymentType() : "Full-Time")
                    .currentStage(newStage != null ? newStage : TimelineStage.REGISTRATION)
                    .salary(extraction.getSalary() != null && !"null".equalsIgnoreCase(extraction.getSalary()) ? extraction.getSalary() : null)
                    .location(extraction.getLocation() != null && !"null".equalsIgnoreCase(extraction.getLocation()) ? extraction.getLocation() : null)
                    .eligibility(extraction.getEligibility() != null && !"null".equalsIgnoreCase(extraction.getEligibility()) ? extraction.getEligibility() : null)
                    .registrationLink(extraction.getRegistrationLink() != null && !"null".equalsIgnoreCase(extraction.getRegistrationLink()) ? extraction.getRegistrationLink() : null)
                    .build();
        }

        timeline.setLastUpdated(LocalDateTime.now());
        timeline.setLatestMessage(message);

        return timelineRepository.save(timeline);
    }

    private Optional<CompanyTimeline> findMatchingTimeline(List<CompanyTimeline> timelines, String targetCompany) {
        for (CompanyTimeline t : timelines) {
            if (companyMatcher.isMatch(t.getCompanyName(), targetCompany, similarityThreshold)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    private boolean shouldAdvanceStage(TimelineStage current, TimelineStage target) {
        if (target == null || target == TimelineStage.OTHER) {
            return false;
        }
        if (current == null || current == TimelineStage.OTHER) {
            return true;
        }
        int currentIndex = FUNNEL_ORDER.indexOf(current);
        int targetIndex = FUNNEL_ORDER.indexOf(target);
        if (currentIndex == -1 || targetIndex == -1) {
            return true;
        }
        return targetIndex >= currentIndex;
    }

    private TimelineStage parseStage(String stageStr) {
        if (stageStr == null || stageStr.isBlank() || "null".equalsIgnoreCase(stageStr)) {
            return TimelineStage.REGISTRATION;
        }
        try {
            return TimelineStage.valueOf(stageStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String lower = stageStr.toLowerCase();
            if (lower.contains("offer") || lower.contains("selected")) return TimelineStage.OFFER;
            if (lower.contains("hr") || lower.contains("managerial")) return TimelineStage.HR;
            if (lower.contains("tech") || lower.contains("coding") || lower.contains("interview")) return TimelineStage.TECHNICAL;
            if (lower.contains("test") || lower.contains("assess") || lower.contains("aptitude")) return TimelineStage.ASSESSMENT;
            if (lower.contains("reject")) return TimelineStage.REJECTED;
            return TimelineStage.REGISTRATION;
        }
    }
}
