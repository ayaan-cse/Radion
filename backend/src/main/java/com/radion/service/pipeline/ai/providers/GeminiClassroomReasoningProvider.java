package com.radion.service.pipeline.ai.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.models.ClassroomAnnouncement;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClassroomReasoningProvider {

    private final ObjectMapper objectMapper;
    private final GeminiProvider geminiProvider;

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
        log.info("Executing Classroom AI Reasoning for CourseWork ID: {}", courseWork.getId());

        String courseName = courseWork.getCourse() != null ? courseWork.getCourse().getName() : "Unknown Course";
        String prompt = String.format(
                "type hint: COURSEWORK\ncourseName: %s\ntitle: %s\ndescription: %s\ndueDate: %s",
                courseName,
                courseWork.getTitle(),
                courseWork.getDescription() != null ? courseWork.getDescription() : "N/A",
                courseWork.getDueDate() != null ? courseWork.getDueDate().toString() : "Not specified"
        );

        return callGemini(prompt, courseWork.getId().toString());
    }

    public ClassroomReasoningResponse evaluateAnnouncement(ClassroomAnnouncement announcement) {
        log.info("Executing Classroom AI Reasoning for Announcement ID: {}", announcement.getId());

        String courseName = announcement.getCourse() != null ? announcement.getCourse().getName() : "Unknown Course";
        String prompt = String.format(
                "type hint: ANNOUNCEMENT\ncourseName: %s\ntitle: (Announcement)\ndescription: %s",
                courseName,
                announcement.getText() != null ? announcement.getText() : "N/A"
        );

        return callGemini(prompt, announcement.getId().toString());
    }

    private ClassroomReasoningResponse callGemini(String prompt, String itemId) {
        try {
            String rawJson = geminiProvider.generateTextWithSystemInstruction(prompt, systemInstruction);

            // Strip markdown code fences if Gemini wraps output
            if (rawJson.startsWith("```json")) rawJson = rawJson.substring(7);
            if (rawJson.startsWith("```"))     rawJson = rawJson.substring(3);
            if (rawJson.endsWith("```"))       rawJson = rawJson.substring(0, rawJson.length() - 3);
            rawJson = rawJson.trim();

            return objectMapper.readValue(rawJson, ClassroomReasoningResponse.class);

        } catch (Exception e) {
            log.error("Gemini evaluation failed for item {}: {}", itemId, e.getMessage());
            throw new RuntimeException("Gemini Classroom API call failed", e);
        }
    }
}

