package com.radion.service.pipeline.ai.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.models.ClassroomCourseWork;
import com.radion.service.pipeline.models.ClassroomReasoningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClassroomReasoningProvider {

    private final ObjectMapper objectMapper;
    private final GeminiProvider geminiProvider; // Reuse base Gemini call capability

    @Value("classpath:ai/classroom_system_instruction.md")
    private Resource systemInstructionResource;

    private String systemInstruction;

    @PostConstruct
    public void init() {
        try {
            systemInstruction = StreamUtils.copyToString(systemInstructionResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load classroom system instructions from classpath", e);
            throw new RuntimeException("Could not initialize Gemini Classroom Reasoning Provider", e);
        }
    }

    public ClassroomReasoningResponse evaluateCourseWork(ClassroomCourseWork courseWork) {
        log.info("Executing Classroom AI Reasoning for course work ID: {}", courseWork.getId());

        try {
            String prompt = String.format("CourseWork Details:\nTitle: %s\nDescription: %s\nDue Date: %s",
                    courseWork.getTitle(),
                    courseWork.getDescription(),
                    courseWork.getDueDate());

            String rawJson = geminiProvider.generateTextWithSystemInstruction(prompt, systemInstruction);

            // Clean markdown blocks if present
            if (rawJson.startsWith("```json")) {
                rawJson = rawJson.substring(7);
            }
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.substring(3);
            }
            if (rawJson.endsWith("```")) {
                rawJson = rawJson.substring(0, rawJson.length() - 3);
            }
            rawJson = rawJson.trim();

            return objectMapper.readValue(rawJson, ClassroomReasoningResponse.class);

        } catch (Exception e) {
            log.error("Failed to evaluate Classroom CourseWork {} via Gemini", courseWork.getId(), e);
            throw new RuntimeException("Gemini Classroom API call failed", e);
        }
    }
}
