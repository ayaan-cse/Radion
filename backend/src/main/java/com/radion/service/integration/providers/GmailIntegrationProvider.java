package com.radion.service.integration.providers;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.repository.MessageRepository;
import com.radion.service.integration.IntegrationProvider;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import com.radion.service.pipeline.placement.PlacementPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailIntegrationProvider implements IntegrationProvider {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final PlacementPipelineOrchestrator placementPipelineOrchestrator;
    private final MessageRepository messageRepository;

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

            int newEmailsCount = 0;
            // 3. Fetch Message Metadata and persist to database
            for (Message msgRef : messages) {
                Message metadataMessage = gmailService.users().messages().get("me", msgRef.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(Arrays.asList("Subject", "From"))
                        .execute();

                var existingOpt = messageRepository.findByUserIdAndExternalId(user.getId(), metadataMessage.getId());
                if (existingOpt.isEmpty()) {
                    String subject = "No Subject";
                    String sender = "Unknown Sender";
                    if (metadataMessage.getPayload() != null && metadataMessage.getPayload().getHeaders() != null) {
                        for (MessagePartHeader header : metadataMessage.getPayload().getHeaders()) {
                            if ("Subject".equalsIgnoreCase(header.getName())) {
                                subject = header.getValue();
                            } else if ("From".equalsIgnoreCase(header.getName())) {
                                sender = header.getValue();
                            }
                        }
                    }

                    boolean isUnread = metadataMessage.getLabelIds() != null && metadataMessage.getLabelIds().contains("UNREAD");
                    String labelsStr = metadataMessage.getLabelIds() != null ? String.join(",", metadataMessage.getLabelIds()) : "";
                    
                    LocalDateTime receivedAt = LocalDateTime.now();
                    if (metadataMessage.getInternalDate() != null) {
                        receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(metadataMessage.getInternalDate()), ZoneId.systemDefault());
                    }

                    com.radion.domain.models.Message dbMessage = com.radion.domain.models.Message.builder()
                            .user(user)
                            .platform(Platform.GMAIL)
                            .externalId(metadataMessage.getId())
                            .title(subject)
                            .sender(sender)
                            .snippet(metadataMessage.getSnippet())
                            .labels(labelsStr)
                            .isUnread(isUnread)
                            .receivedAt(receivedAt)
                            .build();

                    dbMessage = messageRepository.save(dbMessage);
                    newEmailsCount++;

                    // 4. Trigger Placement Intelligence Pipeline
                    try {
                        placementPipelineOrchestrator.processMessage(dbMessage);
                    } catch (Exception e) {
                        log.error("Error running Placement Intelligence Pipeline for message {}", dbMessage.getId(), e);
                    }
                }
            }

            log.info("Successfully fetched {} emails and stored {} new emails in database for user: {}", messages.size(), newEmailsCount, user.getId());

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