package com.radion.service.integration.oauth;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.radion.domain.enums.ConnectionStatus;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.repository.ConnectedServiceRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl {

    private final ConnectedServiceRepository connectedServiceRepository;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    // Granular Scopes required for each integration platform
    private static final Collection<String> GMAIL_SCOPES = Collections.singletonList(
            "https://www.googleapis.com/auth/gmail.readonly"
    );

    private static final Collection<String> CLASSROOM_SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly"
    );

    private static final Collection<String> CALENDAR_SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/calendar.readonly",
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/calendar"
    );

    @Data
    @AllArgsConstructor
    public static class OAuthStatePayload {
        private Platform platform;
        private UUID userId;
        private long createdAtMillis;

        public boolean isExpired() {
            // Expire after 15 minutes (900,000 ms)
            return System.currentTimeMillis() - createdAtMillis > 15 * 60 * 1000L;
        }
    }

    private final Map<String, OAuthStatePayload> stateStore = new ConcurrentHashMap<>();

    public String generateAndStoreStateToken(Platform platform, UUID userId) {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        stateStore.put(stateToken, new OAuthStatePayload(platform, userId, System.currentTimeMillis()));

        // Clean up old expired tokens occasionally to prevent memory leaks
        stateStore.entrySet().removeIf(entry -> entry.getValue().isExpired());

        return stateToken;
    }

    public OAuthStatePayload validateAndConsumeStateToken(String stateToken) {
        if (!StringUtils.hasText(stateToken)) {
            throw new IllegalArgumentException("Missing state token");
        }
        OAuthStatePayload payload = stateStore.remove(stateToken); // consume single-use token
        if (payload == null || payload.isExpired()) {
            throw new SecurityException("Invalid or expired OAuth state token. Possible CSRF attack.");
        }
        return payload;
    }

    public Collection<String> getScopesForPlatform(Platform platform) {
        if (platform == null) return Collections.emptyList();
        return switch (platform) {
            case GMAIL -> GMAIL_SCOPES;
            case GOOGLE_CALENDAR -> CALENDAR_SCOPES;
            case CLASSROOM -> CLASSROOM_SCOPES;
            default -> Collections.emptyList();
        };
    }

    private GoogleAuthorizationCodeFlow getFlow(Collection<String> scopes) {
        return new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), clientId, clientSecret, scopes)
                .setAccessType("offline") // Required to get a refresh token
                .setApprovalPrompt("force")
                .build();
    }

    public String generateAuthorizationUrl(String stateToken, Platform platform) {
        Collection<String> scopes = getScopesForPlatform(platform);
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("No OAuth scopes defined for platform: " + platform);
        }
        return getFlow(scopes).newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(stateToken)
                .build();
    }

    public void exchangeCodeForTokens(String code, ConnectedService connection) {
        try {
            Collection<String> scopes = getScopesForPlatform(connection.getPlatform());
            GoogleTokenResponse response = getFlow(scopes).newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            connection.setAccessToken(response.getAccessToken());
            if (response.getRefreshToken() != null) {
                connection.setRefreshToken(response.getRefreshToken());
            }
            connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
            connection.setStatus(ConnectionStatus.CONNECTED);

            connectedServiceRepository.save(connection);
            log.info("Successfully exchanged code for tokens for connection: {}. Token expiry persisted: {}", connection.getId(), connection.getTokenExpiresAt());

        } catch (IOException e) {
            log.error("Failed to exchange Google OAuth code", e);
            connection.setStatus(ConnectionStatus.ERROR);
            connectedServiceRepository.save(connection);
            throw new RuntimeException("OAuth exchange failed", e);
        }
    }

    public boolean refreshAccessToken(ConnectedService connection) {
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return true; // Token is still valid
        }

        if (connection.getRefreshToken() == null) {
            log.error("No refresh token available for connection: {}", connection.getId());
            connection.setStatus(ConnectionStatus.ERROR);
            connectedServiceRepository.save(connection);
            return false;
        }

        try {
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance(),
                    connection.getRefreshToken(), clientId, clientSecret)
                    .execute();

            connection.setAccessToken(response.getAccessToken());
            connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
            connectedServiceRepository.save(connection);
            
            log.info("Successfully refreshed access token for connection: {}. New token expiry persisted: {}", connection.getId(), connection.getTokenExpiresAt());
            return true;
            
        } catch (TokenResponseException e) {
            log.error("Refresh token revoked or invalid for connection: {}", connection.getId());
            connection.setStatus(ConnectionStatus.DISCONNECTED);
            connectedServiceRepository.save(connection);
            return false;
        } catch (IOException e) {
            log.error("Network error refreshing token", e);
            return false;
        }
    }
}