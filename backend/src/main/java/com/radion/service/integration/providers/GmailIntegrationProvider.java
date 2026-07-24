package com.radion.service.integration.providers;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.service.integration.IntegrationProvider;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import com.radion.service.pipeline.InformationCollectionEngine;
import com.radion.service.pipeline.models.RawPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailIntegrationProvider implements IntegrationProvider {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final InformationCollectionEngine pipelineEngine; // From Step 6B

    @Override
    public Platform getPlatform() {
        return Platform.GMAIL;
    }

    @Override
    public boolean refreshTokenIfNeeded(ConnectedService connection) {
        return googleOAuthService.refreshAccessToken(connection);
    }

    @Override
    public void sync(User user, ConnectedService connection) {
        log.info("Starting real Gmail sync for user: {}", user.getId());
        
        if (!refreshTokenIfNeeded(connection)) {
            log.warn("Skipping Gmail sync due to invalid token for user: {}", user.getId());
            return;
        }

        try {
            Gmail gmailService = buildGmailClient(connection.getAccessToken());
            
            // 1. Incremental Sync Query: Only fetch emails since the last sync
            String query = "is:unread";
            if (connection.getLastSyncAt() != null) {
                long epochSeconds = connection.getLastSyncAt().atZone(ZoneId.systemDefault()).toEpochSecond();
                query += " after:" + epochSeconds;
            }

            // 2. Fetch Message IDs
            ListMessagesResponse response = gmailService.users().messages().list("me")
                    .setQ(query)
                    .setMaxResults(50L) // Limit batch size for performance
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No new emails found for user: {}", user.getId());
                return;
            }

            // 3. Fetch Full Message Payloads and push to AI Pipeline
            for (Message msgRef : messages) {
                Message fullMessage = gmailService.users().messages().get("me", msgRef.getId())
                        .setFormat("full")
                        .execute();

                // Convert to our RawPayload format and send to the Information Collection Engine
                RawPayload payload = RawPayload.builder()
                        .externalMessageId(fullMessage.getId())
                        .platform(Platform.GMAIL)
                        .rawJsonContent(fullMessage.toPrettyString()) // Pass raw JSON to the Parser
                        .build();

                // This triggers the Parser -> AI Engine -> Automation Engine flow
                pipelineEngine.processRawPayload(payload);
            }

            log.info("Successfully synced {} emails for user: {}", messages.size(), user.getId());

        } catch (Exception e) {
            log.error("Gmail API sync failed for user: {}", user.getId(), e);
            throw new RuntimeException("Gmail sync failed", e);
        }
    }

    private Gmail buildGmailClient(String accessToken) {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Radion Dashboard")
                .build();
    }
}