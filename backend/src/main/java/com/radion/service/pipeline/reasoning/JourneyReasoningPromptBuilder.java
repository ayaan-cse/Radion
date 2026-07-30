package com.radion.service.pipeline.reasoning;

import com.radion.domain.models.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Builds the unified system prompt for the Radion Entity-Based Real-World Journey Reasoning Engine.
 * Assembles every LLM request from exactly four versioned and externalized components:
 * 1. Permanent System Instruction (loaded from external resource, never hardcoded in Java).
 * 2. Student's Current World State (active opportunities, stages, tasks, events, documents and relevant history).
 * 3. Incoming Evidence (Email / Post).
 * 4. Strict Business Command JSON Schema (loaded from external resource, never hardcoded in Java).
 *
 * All AI providers (Gemini, GPT, Claude) consume this exact prompt structure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JourneyReasoningPromptBuilder {

    private final ResourceLoader resourceLoader;

    @Value("${radion.ai.system-instruction-path:classpath:ai/system_instruction.md}")
    private String systemInstructionPath;

    @Value("${radion.ai.command-schema-path:classpath:ai/business_command_schema.json}")
    private String commandSchemaPath;

    public String buildPrompt(Message message, String activeContextJson) {
        String systemInstruction = loadResourceContent(systemInstructionPath, "System Instruction");
        String commandSchema = loadResourceContent(commandSchemaPath, "Business Command JSON Schema");

        StringBuilder sb = new StringBuilder();

        // Part 1: Permanent System Instruction
        sb.append("=== PART 1: SYSTEM INSTRUCTION ===\n");
        sb.append(systemInstruction.trim()).append("\n\n");
        sb.append("----------------------------------------------------------------\n\n");

        // Part 2: Student's Current World State
        sb.append("=== PART 2: STUDENT'S CURRENT WORLD STATE (JSON) ===\n");
        sb.append("This JSON represents the student's currently active world state (Opportunities with their timeline history, Pending Action Items, and Upcoming Schedule):\n");
        sb.append("```json\n");
        sb.append(activeContextJson != null && !activeContextJson.trim().isEmpty() ? activeContextJson : "{}");
        sb.append("\n```\n\n");
        sb.append("----------------------------------------------------------------\n\n");

        // Part 3: Incoming Evidence (Email)
        sb.append("=== PART 3: INCOMING EVIDENCE (EMAIL / POST) ===\n");
        sb.append("Sender: ").append(message.getSender() != null ? message.getSender() : "Unknown").append("\n");
        sb.append("Subject: ").append(message.getTitle() != null ? message.getTitle() : "(No Subject)").append("\n");
        sb.append("Received At: ").append(message.getReceivedAt() != null ? message.getReceivedAt().toString() : "Recent").append("\n");
        sb.append("Snippet: ").append(message.getSnippet() != null ? message.getSnippet() : "").append("\n");
        sb.append("Evidence Content:\n");
        String content = message.getRawPayload() != null && !message.getRawPayload().trim().isEmpty() 
            ? message.getRawPayload() 
            : (message.getSnippet() != null ? message.getSnippet() : "");
        sb.append(content.trim());
        sb.append("\n\n");
        sb.append("----------------------------------------------------------------\n\n");

        // Part 4: Strict Business Command JSON Schema
        sb.append("=== PART 4: STRICT BUSINESS COMMAND JSON SCHEMA & OUTPUT FORMAT ===\n");
        sb.append("You MUST return ONLY valid, well-formed JSON conforming exactly to the following JSON schema. Do not include markdown formatting, backticks (` ```json `), or extra commentary outside the JSON:\n");
        sb.append("```json\n");
        sb.append(commandSchema.trim());
        sb.append("\n```\n");

        return sb.toString();
    }

    private String loadResourceContent(String path, String resourceName) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                log.error("Resource {} not found at path: {}", resourceName, path);
                throw new IllegalStateException("Missing required AI configuration resource: " + path);
            }
            return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to load resource {} from path: {}", resourceName, path, e);
            throw new RuntimeException("Could not load AI configuration resource: " + path, e);
        }
    }
}
