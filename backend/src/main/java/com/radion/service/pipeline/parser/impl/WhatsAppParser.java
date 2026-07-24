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

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppParser implements MessageParser {

    private final ObjectMapper objectMapper;

    @Override
    public Platform getSupportedPlatform() {
        return Platform.WHATSAPP;
    }

    @Override
    public NormalizedMessage parse(RawPayload payload) {
        try {
            JsonNode root = objectMapper.readTree(payload.getRawJsonContent());
            
            // Navigate WhatsApp Cloud API JSON structure
            JsonNode entry = root.path("entry").get(0);
            JsonNode changes = entry.path("changes").get(0);
            JsonNode value = changes.path("value");
            JsonNode message = value.path("messages").get(0);
            JsonNode contact = value.path("contacts").get(0);

            String senderName = contact.path("profile").path("name").asText("Unknown Sender");
            String senderPhone = message.path("from").asText();
            String body = message.path("text").path("body").asText("");
            String messageId = message.path("id").asText();
            long timestamp = message.path("timestamp").asLong();

            return NormalizedMessage.builder()
                    .externalId(messageId)
                    .platform(Platform.WHATSAPP)
                    .sender(senderName + " (" + senderPhone + ")")
                    .subject("WhatsApp Message") // WhatsApp doesn't have subjects
                    .body(body)
                    .receivedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse WhatsApp payload", e);
            throw new RuntimeException("WhatsApp parsing failed", e);
        }
    }
}