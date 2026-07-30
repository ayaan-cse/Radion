package com.radion.service.pipeline.reasoning;

import com.radion.domain.enums.Platform;
import com.radion.domain.models.Message;
import com.radion.domain.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JourneyReasoningPromptBuilderTest {

    private JourneyReasoningPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new JourneyReasoningPromptBuilder(new DefaultResourceLoader());
        ReflectionTestUtils.setField(promptBuilder, "systemInstructionPath", "classpath:ai/system_instruction.md");
        ReflectionTestUtils.setField(promptBuilder, "commandSchemaPath", "classpath:ai/business_command_schema.json");
    }

    @Test
    void testBuildPromptContainsAllFourParts() {
        User user = User.builder().id(UUID.randomUUID()).email("student@test.edu").build();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .user(user)
                .platform(Platform.GMAIL)
                .sender("recruiter@tech.com")
                .title("Interview Round 1")
                .snippet("Please pick a slot for your technical interview.")
                .receivedAt(LocalDateTime.now())
                .build();

        String activeContextJson = "{\"opportunities\": [{\"company\": \"Tech Corp\", \"stage\": \"ASSESSMENT\"}]}";

        String prompt = promptBuilder.buildPrompt(message, activeContextJson);

        assertNotNull(prompt);
        assertTrue(prompt.contains("=== PART 1: SYSTEM INSTRUCTION ==="), "Prompt must contain Part 1 System Instruction header");
        assertTrue(prompt.contains("You are the reasoning engine of Radion"), "Prompt must load content from system_instruction.md");
        
        assertTrue(prompt.contains("=== PART 2: STUDENT'S CURRENT WORLD STATE (JSON) ==="), "Prompt must contain Part 2 World State header");
        assertTrue(prompt.contains("Tech Corp"), "Prompt must include active context JSON");

        assertTrue(prompt.contains("=== PART 3: INCOMING EVIDENCE (EMAIL / POST) ==="), "Prompt must contain Part 3 Incoming Evidence header");
        assertTrue(prompt.contains("recruiter@tech.com"), "Prompt must include email sender");
        assertTrue(prompt.contains("Interview Round 1"), "Prompt must include email subject");

        assertTrue(prompt.contains("=== PART 4: STRICT BUSINESS COMMAND JSON SCHEMA & OUTPUT FORMAT ==="), "Prompt must contain Part 4 Schema header");
        assertTrue(prompt.contains("REGISTER_OPPORTUNITY"), "Prompt must load command schema from business_command_schema.json");
    }
}
