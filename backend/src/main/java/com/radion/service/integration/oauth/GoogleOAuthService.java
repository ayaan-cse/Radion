package com.radion.service.integration.oauth;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.radion.domain.enums.ConnectionStatus;
import com.radion.domain.models.ConnectedService;
import com.radion.repository.ConnectedServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

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

    // Scopes required for Gmail and Classroom
    private static final Iterable<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly"
    );

    private GoogleAuthorizationCodeFlow getFlow() {
        return new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), clientId, clientSecret, SCOPES)
                .setAccessType("offline") // Required to get a refresh token
                .setApprovalPrompt("force")
                .build();
    }

    public String generateAuthorizationUrl(String state) {
        return getFlow().newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(state)
                .build();
    }

    public void exchangeCodeForTokens(String code, ConnectedService connection) {
        try {
            GoogleTokenResponse response = getFlow().newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            connection.setAccessToken(response.getAccessToken());
            if (response.getRefreshToken() != null) {
                connection.setRefreshToken(response.getRefreshToken());
            }
            connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresInSeconds()));
            connection.setStatus(ConnectionStatus.CONNECTED);
            
            connectedServiceRepository.save(connection);
            log.info("Successfully exchanged code for tokens for connection: {}", connection.getId());
            
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
            
            log.info("Successfully refreshed access token for connection: {}", connection.getId());
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