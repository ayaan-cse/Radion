package com.radion.web;

import com.radion.domain.enums.ConnectionStatus;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.repository.ConnectedServiceRepository;
import com.radion.repository.UserRepository;
import com.radion.service.integration.SyncManagerService;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final SyncManagerService syncManagerService;
    private final GoogleOAuthServiceImpl googleOAuthService;
    private final UserRepository userRepository;
    private final ConnectedServiceRepository connectedServiceRepository;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/sync")
    public ResponseEntity<Void> syncNow(
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000000") UUID userId) {
        syncManagerService.triggerManualSync(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/google/connect")
    public RedirectView connect(
            @RequestParam Platform platform,
            @RequestParam UUID userId) {
        log.info("Initiating Google OAuth connect for platform {} and userId {}", platform, userId);
        String stateToken = googleOAuthService.generateAndStoreStateToken(platform, userId);
        String authUrl = googleOAuthService.generateAuthorizationUrl(stateToken, platform);
        return new RedirectView(authUrl);
    }

    @GetMapping("/google/callback")
    public RedirectView callback(
            @RequestParam("code") String code,
            @RequestParam("state") String stateToken) {
        log.info("Received Google OAuth callback with state token: {}", stateToken);
        try {
            GoogleOAuthServiceImpl.OAuthStatePayload payload = googleOAuthService.validateAndConsumeStateToken(stateToken);
            Platform platform = payload.getPlatform();
            UUID userId = payload.getUserId();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            ConnectedService connection = connectedServiceRepository.findByUserIdAndPlatform(userId, platform)
                    .orElseGet(() -> ConnectedService.builder()
                            .user(user)
                            .platform(platform)
                            .status(ConnectionStatus.DISCONNECTED)
                            .build());

            googleOAuthService.exchangeCodeForTokens(code, connection);

            // Trigger an immediate initial sync for the newly connected service
            syncManagerService.triggerManualSync(userId);

            return new RedirectView(frontendUrl + "/integrations?connection=success&platform=" + platform.name());
        } catch (Exception e) {
            log.error("Error during Google OAuth callback processing: {}", e.getMessage(), e);
            return new RedirectView(frontendUrl + "/integrations?connection=error&reason=csrf_or_oauth_failed");
        }
    }
}