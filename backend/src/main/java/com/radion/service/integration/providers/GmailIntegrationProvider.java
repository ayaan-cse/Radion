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
import com.radion.service.pipeline.reasoning.JourneyPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import com.google.api.services.gmail.model.MessagePart;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailIntegrationProvider implements IntegrationProvider {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final JourneyPipelineOrchestrator journeyPipelineOrchestrator;
    private final MessageRepository messageRepository;
    private final com.radion.repository.EventRepository eventRepository;
    private final com.radion.repository.TaskRepository taskRepository;

    @Value("${radion.integration.gmail.query:}")
    private String configuredQuery;

    @Override
    public Platform getPlatform() {
        return Platform.GMAIL;
    }

    @Override
    public boolean refreshTokenIfNeeded(ConnectedService connection) {
        return googleOAuthService.refreshAccessToken(connection);
    }

    @Override
    public int sync(User user, ConnectedService connection) {
        log.info("Starting real Gmail sync for user: {}", user.getId());
        
        if (!refreshTokenIfNeeded(connection)) {
            log.warn("Skipping Gmail sync due to invalid token for user: {}", user.getId());
            return 0;
        }

        try {
            Gmail gmailService = buildGmailClient(connection.getAccessToken());
            
            // 1. Determine query for Initial vs. Incremental Sync
            String query = StringUtils.hasText(configuredQuery) ? configuredQuery.trim() : "";
            if (connection.getLastSyncAt() == null) {
                // Failsafe: if somehow lastSyncAt is null, set it to now and do not fetch history!
                connection.setLastSyncAt(LocalDateTime.now());
                log.info("lastSyncAt was null for user {}. Setting to now. Aborting historical sync to respect contract.", user.getId());
                return 0;
            }
            
            // Apply a 120-second (2-minute) safety overlap to catch delayed indexing
            LocalDateTime effectiveAfter = connection.getLastSyncAt().minusSeconds(120);
            long epochSeconds = effectiveAfter.atZone(ZoneId.systemDefault()).toEpochSecond();
            String afterClause = "after:" + epochSeconds;
            query = query.isEmpty() ? afterClause : query + " " + afterClause;
            
            log.info("GMAIL-DEBUG: lastSyncAt = {}", connection.getLastSyncAt());
            log.info("GMAIL-DEBUG: epochSeconds = {}", epochSeconds);
            log.info("GMAIL-DEBUG: Exact Gmail query being executed: '{}'", query);

            String pageToken = null;
            int totalFetched = 0;
            int duplicatesSkipped = 0;
            int newEmailsCount = 0;
            int processedCount = 0;
            int ignoredCount = 0;
            int failedCount = 0;

            long eventsBefore = eventRepository.count();
            long tasksBefore = taskRepository.count();

            do {
                log.info("GMAIL-DEBUG: Fetching page with token: {}", pageToken);
                Gmail.Users.Messages.List listRequest = gmailService.users().messages().list("me")
                        .setMaxResults(100L);
                if (StringUtils.hasText(query)) {
                    listRequest.setQ(query);
                }
                if (pageToken != null) {
                    listRequest.setPageToken(pageToken);
                }

                ListMessagesResponse response = listRequest.execute();
                List<Message> messages = response.getMessages();
                if (messages == null || messages.isEmpty()) {
                    log.info("GMAIL-DEBUG: Gmail API returned NO messages for this page.");
                    break;
                }

                log.info("GMAIL-DEBUG: Gmail API returned {} messages on this page.", messages.size());
                totalFetched += messages.size();

                for (Message msgRef : messages) {
                    log.info("GMAIL-DEBUG: Processing fetched message ID: {}", msgRef.getId());
                    
                    var existingOpt = messageRepository.findByUserIdAndExternalId(user.getId(), msgRef.getId());
                    if (existingOpt.isPresent()) {
                        log.info("GMAIL-DEBUG: Skipping message ID {} - ALREADY EXISTS in database (duplicate).", msgRef.getId());
                        duplicatesSkipped++;
                        continue;
                    }

                    log.info("GMAIL-DEBUG: Message ID {} is NEW. Fetching full payload from Gmail API...", msgRef.getId());
                    Message metadataMessage = gmailService.users().messages().get("me", msgRef.getId())
                            .setFormat("full")
                            .execute();

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

                    log.info("GMAIL-DEBUG: Fetched full payload for message ID {}. Subject: '{}', Sender: '{}'", msgRef.getId(), subject, sender);

                    boolean isUnread = metadataMessage.getLabelIds() != null && metadataMessage.getLabelIds().contains("UNREAD");
                    String labelsStr = metadataMessage.getLabelIds() != null ? String.join(",", metadataMessage.getLabelIds()) : "";
                    
                    LocalDateTime receivedAt = LocalDateTime.now();
                    if (metadataMessage.getInternalDate() != null) {
                        receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(metadataMessage.getInternalDate()), ZoneId.systemDefault());
                    }

                    String body = extractBodyFromPayload(metadataMessage.getPayload());
                    if (body == null || body.isBlank()) {
                        body = metadataMessage.getSnippet();
                    }

                    com.radion.domain.models.Message dbMessage = com.radion.domain.models.Message.builder()
                            .user(user)
                            .platform(Platform.GMAIL)
                            .externalId(metadataMessage.getId())
                            .title(subject)
                            .sender(sender)
                            .snippet(metadataMessage.getSnippet())
                            .rawPayload(body)
                            .labels(labelsStr)
                            .isUnread(isUnread)
                            .receivedAt(receivedAt)
                            .build();

                    dbMessage = messageRepository.save(dbMessage);
                    log.info("GMAIL-DEBUG: Saved message ID {} to database.", msgRef.getId());
                    newEmailsCount++;

                    // Trigger Journey Reasoning Pipeline
                    try {
                        log.info("GMAIL-DEBUG: Sending message ID {} to JourneyPipelineOrchestrator...", msgRef.getId());
                        journeyPipelineOrchestrator.processMessage(dbMessage);
                        log.info("GMAIL-DEBUG: Message ID {} successfully processed by JourneyPipelineOrchestrator.", msgRef.getId());
                        
                        com.radion.domain.enums.MessageProcessingState state = dbMessage.getProcessingState();
                        if (state == com.radion.domain.enums.MessageProcessingState.IGNORED) {
                            ignoredCount++;
                        } else if (state == com.radion.domain.enums.MessageProcessingState.FAILED || state == com.radion.domain.enums.MessageProcessingState.PERMANENTLY_FAILED) {
                            failedCount++;
                        } else {
                            processedCount++;
                        }
                    } catch (Exception e) {
                        log.error("GMAIL-DEBUG: Error running Journey Reasoning Pipeline for message {}", dbMessage.getId(), e);
                        failedCount++;
                    }
                }

                pageToken = response.getNextPageToken();
            } while (pageToken != null);

            long eventsCreated = eventRepository.count() - eventsBefore;
            long tasksCreated = taskRepository.count() - tasksBefore;

            log.info("===== GMAIL SYNC =====");
            log.info("Current lastSyncAt: {}", connection.getLastSyncAt());
            log.info("Effective query timestamp: {}", epochSeconds);
            log.info("Final Gmail query: {}", query);
            log.info("Messages fetched: {}", totalFetched);
            log.info("Duplicate messages skipped: {}", duplicatesSkipped);
            log.info("Processed messages: {}", processedCount);
            log.info("Ignored messages: {}", ignoredCount);
            log.info("Failed messages: {}", failedCount);
            log.info("Events created: {}", eventsCreated);
            log.info("Tasks created: {}", tasksCreated);
            log.info("Calendar sync result: Triggered automatically by pipeline");
            log.info("Updated lastSyncAt: Will be updated by SyncManagerService");
            log.info("======================");

            return totalFetched;

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

    private String extractBodyFromPayload(MessagePart part) {
        if (part == null) return "";
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] decoded = Base64.getUrlDecoder().decode(part.getBody().getData());
            return new String(decoded, StandardCharsets.UTF_8);
        }
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                if ("text/plain".equalsIgnoreCase(subPart.getMimeType())) {
                    if (subPart.getBody() != null && subPart.getBody().getData() != null) {
                        byte[] decoded = Base64.getUrlDecoder().decode(subPart.getBody().getData());
                        return new String(decoded, StandardCharsets.UTF_8);
                    }
                }
            }
            for (MessagePart subPart : part.getParts()) {
                String text = extractBodyFromPayload(subPart);
                if (!text.isEmpty()) return text;
            }
        }
        return "";
    }
}