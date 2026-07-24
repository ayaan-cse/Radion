package com.radion.service.pipeline.ai;

import com.radion.service.pipeline.models.NormalizedMessage;
import org.springframework.stereotype.Component;

@Component
public class PromptManager {

    private static final String SYSTEM_PROMPT = """
        You are a highly accurate AI extraction engine for a student productivity dashboard.
        Analyze the provided message (email/announcement) and extract key information into a STRICT JSON format.
        Do NOT wrap the response in markdown blocks (e.g., ```json). Return ONLY the raw JSON object.
        
        Classification Rules:
        - EVENT: Interviews, placement drives, meetings, exams.
        - TASK: Assignments, registrations, document submissions.
        - REMINDER: Follow-ups, approaching deadlines.
        - IGNORE: Spam, casual conversation, non-actionable info.
        
        JSON Schema to follow exactly:
        {
          "classification": "EVENT|TASK|REMINDER|IGNORE",
          "category": "INTERVIEW|MEETING|DEADLINE|TASK",
          "companyName": "string or null",
          "assignmentName": "string or null",
          "subject": "string",
          "role": "string or null",
          "ctc": "string or null",
          "eligibilityCriteria": ["string"],
          "requiredDocuments": ["string"],
          "interviewRounds": "string or null",
          "eventDate": "YYYY-MM-DD or null",
          "eventTime": "HH:MM:SS or null",
          "priority": "HIGH|MEDIUM|LOW",
          "actionRequired": boolean,
          "actionItems": ["string"],
          "meetingLinks": ["string"],
          "registrationLink": "string or null",
          "location": "string or null",
          "summary": "1 sentence summary",
          "confidenceScore": 0.0 to 1.0
        }
        """;

    public String buildPrompt(NormalizedMessage message) {
        return SYSTEM_PROMPT + "\n\n" +
               "Platform: " + message.getPlatform() + "\n" +
               "Sender: " + message.getSender() + "\n" +
               "Subject: " + message.getSubject() + "\n" +
               "Body: " + message.getBody();
    }
}