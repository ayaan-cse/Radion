package com.radion.web;

import com.radion.domain.models.User;
import com.radion.repository.UserRepository;
import com.radion.service.dev.DeveloperTestingService;
import com.radion.repository.ConnectedServiceRepository;
import com.radion.service.dev.dto.DryRunJourneyReasoningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/dev/testing")
@RequiredArgsConstructor
public class DeveloperTestingController {

    private final DeveloperTestingService developerTestingService;
    private final UserRepository userRepository;
    private final ConnectedServiceRepository connectedServiceRepository;

    @GetMapping({"/classification/dry-run", "/journey-reasoning/dry-run"})
    public ResponseEntity<DryRunJourneyReasoningResponse> getDryRunResults(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = resolveUserId(authentication);
        log.info("GET request for AI Journey Reasoning dry run: userId={}, limit={}", userId, limit);
        DryRunJourneyReasoningResponse response = developerTestingService.executeDryRun(userId, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/classification/dry-run", "/journey-reasoning/dry-run"})
    public ResponseEntity<DryRunJourneyReasoningResponse> postDryRunResults(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = resolveUserId(authentication);
        log.info("POST request for AI Journey Reasoning dry run: userId={}, limit={}", userId, limit);
        DryRunJourneyReasoningResponse response = developerTestingService.executeDryRun(userId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reset-oauth")
    public ResponseEntity<String> resetOAuth(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        User user = userRepository.findById(userId).orElseThrow();
        
        // 1. Delete Dashboard Login Calendar tokens
        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleTokenExpiresAt(null);
        userRepository.save(user);
        
        // 2. Delete all ConnectedService records for Gmail/Classroom
        connectedServiceRepository.deleteAll(connectedServiceRepository.findByUserId(userId));
        
        log.info("Successfully reset OAuth state for user {}", user.getEmail());
        return ResponseEntity.ok("Successfully wiped all Google OAuth tokens and ConnectedService records for " + user.getEmail() + ". Your application data was not modified. You may now perform a clean login from the dashboard.");
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (Exception e) {
                log.warn("Could not parse UUID from authentication principal: {}", authentication.getName());
            }
        }
        // In dev mode when JWT auth filter is bypassed, default to the first existing user in the database
        return userRepository.findAll().stream()
                .findFirst()
                .map(User::getId)
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }
}
