package com.radion.web;

import com.radion.domain.enums.ConnectionStatus;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.dto.UserSyncRequest;
import com.radion.repository.ConnectedServiceRepository;
import com.radion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final ConnectedServiceRepository connectedServiceRepository;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncUser(@RequestBody UserSyncRequest request) {
        log.info("--- /auth/sync CALLED ---");
        log.info("Received request for email: {}", request.getEmail());
        log.info("Has googleAccessToken in DTO: {}", request.getGoogleAccessToken() != null && !request.getGoogleAccessToken().isEmpty());
        log.info("Has googleRefreshToken in DTO: {}", request.getGoogleRefreshToken() != null && !request.getGoogleRefreshToken().isEmpty());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .avatarUrl(request.getAvatarUrl())
                            .build();
                    return userRepository.save(newUser);
                });

        boolean updated = false;
        if (request.getGoogleAccessToken() != null && !request.getGoogleAccessToken().equals(user.getGoogleAccessToken())) {
            user.setGoogleAccessToken(request.getGoogleAccessToken());
            updated = true;
        }
        if (request.getGoogleRefreshToken() != null && !request.getGoogleRefreshToken().equals(user.getGoogleRefreshToken())) {
            user.setGoogleRefreshToken(request.getGoogleRefreshToken());
            updated = true;
        }

        LocalDateTime tokenExpiresAt = null;
        if (request.getGoogleTokenExpiresAt() != null) {
            tokenExpiresAt = Instant.ofEpochMilli(request.getGoogleTokenExpiresAt())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            user.setGoogleTokenExpiresAt(tokenExpiresAt);
            updated = true;
        }

        if (updated) {
            log.info("Updating user tokens in DB...");
            try {
                userRepository.save(user);
                log.info("Successfully updated user tokens in DB.");
            } catch (Exception e) {
                log.error("Failed to save user tokens to DB! Exception: ", e);
                throw e;
            }

            // Auto-bootstrap Classroom ConnectedService if Google tokens are present.
            // The same OAuth grant that includes Calendar also includes Classroom scopes.
            if (request.getGoogleAccessToken() != null && request.getGoogleRefreshToken() != null) {
                bootstrapClassroomConnection(user, request.getGoogleAccessToken(),
                        request.getGoogleRefreshToken(), tokenExpiresAt, request.getEmail());
            }
        } else {
            log.info("No tokens were updated (either null or already matched).");
        }

        return ResponseEntity.ok(Map.of("id", user.getId().toString()));
    }

    /**
     * Creates a CLASSROOM ConnectedService row if one doesn't exist yet.
     * Called automatically on every Google login that brings a new access token.
     */
    private void bootstrapClassroomConnection(User user, String accessToken, String refreshToken,
                                               LocalDateTime tokenExpiresAt, String accountEmail) {
        try {
            ConnectedService existing = connectedServiceRepository
                    .findByUserIdAndPlatform(user.getId(), Platform.CLASSROOM)
                    .orElse(null);

            if (existing == null) {
                ConnectedService classroomService = ConnectedService.builder()
                        .user(user)
                        .platform(Platform.CLASSROOM)
                        .status(ConnectionStatus.CONNECTED)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenExpiresAt(tokenExpiresAt)
                        .accountEmail(accountEmail)
                        .grantedScopes("https://www.googleapis.com/auth/classroom.courses.readonly " +
                                "https://www.googleapis.com/auth/classroom.coursework.me.readonly " +
                                "https://www.googleapis.com/auth/classroom.announcements.readonly")
                        .build();
                connectedServiceRepository.save(classroomService);
                log.info("Auto-bootstrapped CLASSROOM ConnectedService for user: {}", user.getId());
            } else {
                // Always refresh the token so Classroom sync uses the latest credentials
                existing.setAccessToken(accessToken);
                existing.setRefreshToken(refreshToken);
                existing.setTokenExpiresAt(tokenExpiresAt);
                existing.setStatus(ConnectionStatus.CONNECTED);
                connectedServiceRepository.save(existing);
                log.info("Updated CLASSROOM ConnectedService tokens for user: {}", user.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to bootstrap CLASSROOM ConnectedService for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
