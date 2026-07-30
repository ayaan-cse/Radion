package com.radion.web;

import com.radion.domain.models.User;
import com.radion.dto.UserSyncRequest;
import com.radion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;

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
        if (request.getGoogleTokenExpiresAt() != null) {
            java.time.LocalDateTime expiresAt = java.time.Instant.ofEpochMilli(request.getGoogleTokenExpiresAt())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            user.setGoogleTokenExpiresAt(expiresAt);
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
        } else {
            log.info("No tokens were updated (either null or already matched).");
        }

        return ResponseEntity.ok(Map.of("id", user.getId().toString()));
    }
}
