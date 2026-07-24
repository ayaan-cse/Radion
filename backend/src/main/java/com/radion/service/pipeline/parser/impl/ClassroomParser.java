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
public class ClassroomParser implements MessageParser {

    private final ObjectMapper objectMapper;

    @Override
    public Platform getSupportedPlatform() {
        return Platform.CLASSROOM;
    }

    @Override
    public NormalizedMessage parse(RawPayload payload) {
        try {
            JsonNode root = objectMapper.readTree(payload.getRawJsonContent());
            String type = root.path("type").asText();
            String courseName = root.path("courseName").asText();
            String updateTimeStr = root.path("updateTime").asText();
            JsonNode item = root.path("item");

            LocalDateTime receivedAt = LocalDateTime.ofInstant(Instant.parse(updateTimeStr), ZoneId.systemDefault());
            
            String subject;
            StringBuilder bodyBuilder = new StringBuilder();

            if ("COURSE_WORK".equals(type)) {
                subject = "Assignment: " + item.path("title").asText();
                bodyBuilder.append(item.path("description").asText("No description provided."));
                
                // Extract Due Date if present to help the AI Engine
                JsonNode dueDate = item.path("dueDate");
                JsonNode dueTime = item.path("dueTime");
                if (!dueDate.isMissingNode()) {
                    bodyBuilder.append("\n\nDue Date: ")
                            .append(dueDate.path("year").asText()).append("-")
                            .append(String.format("%02d", dueDate.path("month").asInt())).append("-")
                            .append(String.format("%02d", dueDate.path("day").asInt()));
                    
                    if (!dueTime.isMissingNode()) {
                        bodyBuilder.append(" ")
                                .append(String.format("%02d", dueTime.path("hours").asInt())).append(":")
                                .append(String.format("%02d", dueTime.path("minutes").asInt()));
                    }
                }
            } else if ("ANNOUNCEMENT".equals(type)) {
                subject = "Announcement in " + courseName;
                bodyBuilder.append(item.path("text").asText(""));
            } else {
                throw new IllegalArgumentException("Unknown Classroom item type: " + type);
            }

            return NormalizedMessage.builder()
                    .externalId(payload.getExternalMessageId())
                    .platform(Platform.CLASSROOM)
                    .sender(courseName) // Use course name as the sender for context
                    .subject(subject)
                    .body(bodyBuilder.toString())
                    .receivedAt(receivedAt)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Classroom payload for ID: {}", payload.getExternalMessageId(), e);
            throw new RuntimeException("Classroom parsing failed", e);
        }
    }
}