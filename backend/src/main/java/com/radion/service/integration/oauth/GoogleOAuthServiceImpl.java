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
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

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
    private static final Collection<String> GMAIL_SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "openid"
    );

    private static final Collection<String> CLASSROOM_SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "openid"
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
            case CLASSROOM -> CLASSROOM_SCOPES;
            default -> Collections.emptyList();
        };
    }

    private GoogleAuthorizationCodeFlow getFlow(Collection<String> scopes) {
        return new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), clientId, clientSecret, scopes)
                .setAccessType("offline") // Required to get a refresh token
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
                .set("prompt", "select_account consent")
                .set("include_granted_scopes", "true")
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
            connection.setGrantedScopes(response.getScope());
            connection.setStatus(ConnectionStatus.CONNECTED);

            // Fetch connected Google account profile info
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(response.getAccessToken());
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> userInfoRes = restTemplate.exchange(
                        "https://www.googleapis.com/oauth2/v3/userinfo",
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                if (userInfoRes.getBody() != null) {
                    Map<String, Object> userInfo = userInfoRes.getBody();
                    String email = (String) userInfo.get("email");
                    String name = (String) userInfo.get("name");
                    if (!StringUtils.hasText(name)) {
                        String givenName = (String) userInfo.get("given_name");
                        String familyName = (String) userInfo.get("family_name");
                        if (StringUtils.hasText(givenName)) {
                            name = givenName + (StringUtils.hasText(familyName) ? " " + familyName : "");
                        } else {
                            name = email;
                        }
                    }
                    connection.setAccountEmail(email);
                    connection.setAccountName(name);
                    connection.setAccountAvatarUrl((String) userInfo.get("picture"));
                    connection.setExternalAccountId((String) userInfo.get("sub"));
                    log.info("Fetched connected account profile for platform {}: email={}, name={}",
                            connection.getPlatform(), connection.getAccountEmail(), connection.getAccountName());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch userinfo for connected account during OAuth exchange", e);
            }

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

    public boolean refreshUserAccessToken(com.radion.domain.models.User user) {
        if (user.getGoogleTokenExpiresAt() != null && user.getGoogleTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return true; // Token is still valid
        }
        if (user.getGoogleRefreshToken() == null) {
            log.warn("No refresh token available for user: {}", user.getId());
            return false;
        }
        try {
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance(),
                    user.getGoogleRefreshToken(), clientId, clientSecret)
                    .execute();
            user.setGoogleAccessToken(response.getAccessToken());
            user.setGoogleTokenExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
            log.info("Successfully refreshed Dashboard token for user: {}", user.getId());
            return true;
        } catch (TokenResponseException e) {
            // invalid_grant = permanent failure — user must re-authenticate via Dashboard
            String errorCode = e.getDetails() != null ? e.getDetails().getError() : "unknown";
            log.error("[REAUTH_REQUIRED] Dashboard OAuth token permanently invalid for user: {}. " +
                      "Error: {}. User must reconnect via Dashboard login. " +
                      "Likely cause: OAuth app in Testing mode (tokens expire after 7 days) " +
                      "or user revoked access.",
                      user.getId(), errorCode);
            // Clear invalid tokens so the frontend knows the user needs to re-login
            user.setGoogleAccessToken(null);
            user.setGoogleRefreshToken(null);
            user.setGoogleTokenExpiresAt(null);
            throw new InvalidGrantException("Dashboard Google token invalid (" + errorCode + "). Re-authentication required.");
        } catch (IOException e) {
            log.error("Network error refreshing Dashboard token for user: {}", user.getId(), e);
            return false;
        }
    }

    /**
     * Thrown when Google returns invalid_grant — permanent, not retryable.
     * The user must log out and log back in via the Dashboard to issue a fresh token.
     */
    public static class InvalidGrantException extends RuntimeException {
        public InvalidGrantException(String message) {
            super(message);
        }
    }
}