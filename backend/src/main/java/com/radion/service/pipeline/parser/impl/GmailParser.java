package com.radion.service.pipeline.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.Platform;
import com.radion.service.pipeline.models.NormalizedMessage;
import com.radion.service.pipeline.models.RawPayload;
import com.radion.service.pipeline.parser.MessageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailParser implements MessageParser {

    private final ObjectMapper objectMapper;

    @Override
    public Platform getSupportedPlatform() {
        return Platform.GMAIL;
    }

    @Override
    public NormalizedMessage parse(RawPayload payload) {
        try {
            JsonNode root = objectMapper.readTree(payload.getRawJsonContent());
            JsonNode payloadNode = root.path("payload");
            JsonNode headers = payloadNode.path("headers");

            String subject = extractHeader(headers, "Subject");
            String sender = extractHeader(headers, "From");
            
            // Extract timestamp
            long internalDate = root.path("internalDate").asLong();
            LocalDateTime receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(internalDate), ZoneId.systemDefault());

            // Extract body (handling multipart/alternative)
            String body = extractBody(payloadNode);

            return NormalizedMessage.builder()
                    .externalId(payload.getExternalMessageId())
                    .platform(Platform.GMAIL)
                    .sender(sender)
                    .subject(subject)
                    .body(body)
                    .receivedAt(receivedAt)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Gmail payload for ID: {}", payload.getExternalMessageId(), e);
            throw new RuntimeException("Gmail parsing failed", e);
        }
    }

    private String extractHeader(JsonNode headers, String name) {
        for (JsonNode header : headers) {
            if (name.equalsIgnoreCase(header.path("name").asText())) {
                return header.path("value").asText();
            }
        }
        return "Unknown";
    }

    private String extractBody(JsonNode payloadNode) {
        // Simple extraction: check if body data exists at root level
        JsonNode bodyNode = payloadNode.path("body").path("data");
        if (!bodyNode.isMissingNode()) {
            return decodeBase64Url(bodyNode.asText());
        }

        // If multipart, iterate through parts to find text/plain
        JsonNode parts = payloadNode.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if ("text/plain".equals(part.path("mimeType").asText())) {
                    return decodeBase64Url(part.path("body").path("data").asText());
                }
            }
        }
        return "No readable text found.";
    }

    private String decodeBase64Url(String base64Url) {
        if (base64Url == null || base64Url.isEmpty()) return "";
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64Url);
        return new String(decodedBytes);
    }
}