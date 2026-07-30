package com.radion.service.pipeline.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.MessageProcessingState;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.*;
import com.radion.repository.*;
import com.radion.service.pipeline.models.BusinessCommand;
import com.radion.service.pipeline.models.JourneyReasoningResponse;
import com.radion.service.pipeline.models.NormalizedMessage;
import com.radion.service.pipeline.models.RawPayload;
import com.radion.service.pipeline.parser.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Unified Orchestrator for the Radion Entity-Based Real-World Journey Reasoning Engine.
 * Manages the student's evolving world state by feeding evidence and active context into AI reasoning,
 * and translating predicted Business Commands into transactional database operations via WorldStateTranslationEngine.
 */
@Slf4j
@Service
public class JourneyPipelineOrchestrator {

    private final MessageRepository messageRepository;
    private final AIProcessingLogRepository aiProcessingLogRepository;
    private final CompanyTimelineRepository timelineRepository;
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JourneyReasoningAIProvider reasoningProvider;
    private final WorldStateTranslationEngine translationEngine;
    private final ObjectMapper objectMapper;
    private final Map<Platform, MessageParser> parsers;

    public JourneyPipelineOrchestrator(
            MessageRepository messageRepository,
            AIProcessingLogRepository aiProcessingLogRepository,
            CompanyTimelineRepository timelineRepository,
            TaskRepository taskRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            JourneyReasoningAIProvider reasoningProvider,
            WorldStateTranslationEngine translationEngine,
            ObjectMapper objectMapper,
            List<MessageParser> parserList) {
        this.messageRepository = messageRepository;
        this.aiProcessingLogRepository = aiProcessingLogRepository;
        this.timelineRepository = timelineRepository;
        this.taskRepository = taskRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.reasoningProvider = reasoningProvider;
        this.translationEngine = translationEngine;
        this.objectMapper = objectMapper;
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(MessageParser::getSupportedPlatform, Function.identity()));
    }

    /**
     * Executes the Entity-Based Journey Reasoning Pipeline on a stored Message.
     * Guaranteed never to process the same message twice.
     */
    public void processMessage(Message message) {
        log.info("Starting Journey Reasoning Pipeline for message ID: {}", message.getId());

        if (message.getProcessingState() != null &&
            message.getProcessingState() != MessageProcessingState.NEW &&
            message.getProcessingState() != MessageProcessingState.FAILED) {
            log.warn("Skipping message {}: Already processed in state {}. Never process the same email twice.",
                     message.getId(), message.getProcessingState());
            return;
        }

        JourneyReasoningResponse reasoningResult = null;
        try {
            message.setProcessingState(MessageProcessingState.NEW);
            messageRepository.save(message);

            User user = message.getUser();
            if (user == null) {
                user = resolveDefaultUser();
                message.setUser(user);
            }

            String activeContextJson = loadActiveContext(user);
            reasoningResult = reasoningProvider.reasonOverMessage(message, activeContextJson);

            message.setReasoningSummary(reasoningResult.getEmailSummary());
            message.setMutationCount(reasoningResult.getCommands() != null ? reasoningResult.getCommands().size() : 0);

            if (reasoningResult.isError()) {
                log.warn("AI Reasoning returned error for message {}: {}", message.getId(), reasoningResult.getErrorMessage());
                handlePipelineFailure(message, reasoningResult.getErrorMessage(), reasoningResult);
                return;
            }

            if (!reasoningResult.isHasJourneyImpact() || reasoningResult.getCommands().isEmpty()) {
                log.info("Message {} has no actionable journey impact. State -> IGNORED.", message.getId());
                message.setProcessingState(MessageProcessingState.IGNORED);
                messageRepository.save(message);
                saveAIProcessingLog(message, reasoningResult);
                return;
            }

            int executedCount = translationEngine.executeCommands(user, message, reasoningResult.getCommands());

            if (reasoningResult.isUncertainty()) {
                message.setProcessingState(MessageProcessingState.MANUAL_REVIEW);
            } else if (executedCount > 0) {
                message.setProcessingState(MessageProcessingState.TIMELINE_UPDATED);
            } else {
                message.setProcessingState(MessageProcessingState.AI_PROCESSED);
            }

            messageRepository.save(message);
            saveAIProcessingLog(message, reasoningResult);
            log.info("Successfully completed Journey Reasoning Pipeline for message {}. Final State: {}",
                     message.getId(), message.getProcessingState());

        } catch (Exception e) {
            log.error("Journey Reasoning Pipeline failed for message {}", message.getId(), e);
            
            if (reasoningResult == null) {
                reasoningResult = new JourneyReasoningResponse();
                reasoningResult.setError(true);
                reasoningResult.setErrorMessage("Pipeline crashed before AI reasoning completed: " + e.getMessage());
            } else {
                reasoningResult.setError(true);
                reasoningResult.setErrorMessage("Execution Failed: " + e.getMessage());
            }
            
            handlePipelineFailure(message, e.getMessage() != null ? e.getMessage() : "Unknown Error", reasoningResult);
        }
    }

    private void handlePipelineFailure(Message message, String errorString, JourneyReasoningResponse reasoningResult) {
        String lowerError = errorString.toLowerCase();
        boolean isTemporary = lowerError.contains("429") || 
                              lowerError.contains("too many requests") || 
                              lowerError.contains("500") || 
                              lowerError.contains("502") || 
                              lowerError.contains("503") || 
                              lowerError.contains("504") || 
                              lowerError.contains("timeout") || 
                              lowerError.contains("connection refused") ||
                              lowerError.contains("resource_exhausted");

        if (isTemporary) {
            int retryCount = message.getRetryCount();
            int maxRetries = 5;
            
            if (retryCount >= maxRetries) {
                log.error("Message {} failed after {} retries. Marking as PERMANENTLY_FAILED.", message.getId(), retryCount);
                message.setProcessingState(MessageProcessingState.PERMANENTLY_FAILED);
                message.setNextRetryAt(null);
            } else {
                int[] backoffMinutes = {5, 15, 30, 60, 120};
                int delayMinutes = backoffMinutes[retryCount];
                message.setRetryCount(retryCount + 1);
                message.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                message.setProcessingState(MessageProcessingState.FAILED);
                log.warn("Message {} failed (Attempt {}). Will retry in {} minutes at {}", 
                         message.getId(), message.getRetryCount(), delayMinutes, message.getNextRetryAt());
            }
        } else {
            log.error("Message {} encountered a permanent failure. Bypassing retry queue. Error: {}", message.getId(), errorString);
            message.setProcessingState(MessageProcessingState.PERMANENTLY_FAILED);
            message.setNextRetryAt(null);
        }

        try {
            messageRepository.save(message);
        } catch (Exception ex) {
            log.error("Failed to save FAILED/PERMANENTLY_FAILED state for message {}", message.getId(), ex);
        }
        
        saveAIProcessingLog(message, reasoningResult);
    }

    /**
     * Entry point for raw payloads (Google Classroom, Webhooks).
     */
    public void processRawPayload(RawPayload payload) {
        processRawPayload(payload, null);
    }

    /**
     * Entry point for raw payloads with an explicit user.
     */
    public void processRawPayload(RawPayload payload, User user) {
        log.info("Received raw payload from platform: {}", payload.getPlatform());

        MessageParser parser = parsers.get(payload.getPlatform());
        if (parser == null) {
            log.error("No parser found for platform: {}", payload.getPlatform());
            return;
        }

        NormalizedMessage normalized = parser.parse(payload);
        if (normalized == null) {
            log.warn("Failed to parse normalized message from payload.");
            return;
        }

        if (user == null) {
            user = resolveDefaultUser();
        }

        Message message = Message.builder()
                .user(user)
                .platform(normalized.getPlatform())
                .externalId(normalized.getExternalId() != null ? normalized.getExternalId() : UUID.randomUUID().toString())
                .title(normalized.getSubject() != null ? normalized.getSubject() : "(No Title)")
                .sender(normalized.getSender() != null ? normalized.getSender() : payload.getPlatform().name())
                .snippet(normalized.getBody() != null && normalized.getBody().length() > 500 ? normalized.getBody().substring(0, 500) : normalized.getBody())
                .rawPayload(payload.getRawJsonContent())
                .receivedAt(normalized.getReceivedAt() != null ? normalized.getReceivedAt() : LocalDateTime.now())
                .processingState(MessageProcessingState.NEW)
                .build();

        message = messageRepository.save(message);
        processMessage(message);
    }

    private String loadActiveContext(User user) {
        if (user == null) return "{}";
        try {
            List<CompanyTimeline> timelines = timelineRepository.findByUserIdOrderByLastUpdatedDesc(user.getId());
            List<Task> tasks = taskRepository.findByUserIdAndIsCompletedFalse(user.getId());
            List<Event> events = eventRepository.findByUserIdAndEventTimeAfterOrderByEventTimeAsc(user.getId(), LocalDateTime.now().minusDays(1));

            Map<String, Object> contextMap = Map.of(
                "activeOpportunities", timelines.stream().limit(10).map(t -> Map.of(
                    "companyName", t.getCompanyName() != null ? t.getCompanyName() : "",
                    "currentStage", t.getCurrentStage() != null ? t.getCurrentStage().name() : "",
                    "role", t.getRole() != null ? t.getRole() : ""
                )).toList(),
                "pendingActionItems", tasks.stream().limit(15).map(t -> Map.of(
                    "title", t.getTitle() != null ? t.getTitle() : "",
                    "dueDate", t.getDueDate() != null ? t.getDueDate().toString() : ""
                )).toList(),
                "upcomingSchedule", events.stream().limit(15).map(e -> Map.of(
                    "title", e.getTitle() != null ? e.getTitle() : "",
                    "companyOrSource", e.getCompanyOrSource() != null ? e.getCompanyOrSource() : "",
                    "scheduledTime", e.getEventTime() != null ? e.getEventTime().toString() : ""
                )).toList()
            );
            return objectMapper.writeValueAsString(contextMap);
        } catch (Exception e) {
            log.warn("Failed to serialize active context for user {}", user.getId(), e);
            return "{}";
        }
    }

    private User resolveDefaultUser() {
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private void saveAIProcessingLog(Message message, JourneyReasoningResponse result) {
        try {
            AIProcessingLog existingLog = aiProcessingLogRepository.findByMessageId(message.getId()).orElse(null);
            if (existingLog != null) {
                existingLog.setExtractedJson(objectMapper.writeValueAsString(result));
                existingLog.setAiSummary(result.getEmailSummary() != null ? result.getEmailSummary() : "Journey Reasoning Evaluated");
                existingLog.setConfidenceScore(result.isUncertainty() ? 0.50 : 0.95);
                aiProcessingLogRepository.save(existingLog);
            } else {
                AIProcessingLog logEntry = AIProcessingLog.builder()
                        .message(message)
                        .extractedJson(objectMapper.writeValueAsString(result))
                        .aiSummary(result.getEmailSummary() != null ? result.getEmailSummary() : "Journey Reasoning Evaluated")
                        .confidenceScore(result.isUncertainty() ? 0.50 : 0.95)
                        .build();
                aiProcessingLogRepository.save(logEntry);
            }
        } catch (Exception e) {
            log.warn("Could not save AIProcessingLog for message {}", message.getId(), e);
        }
    }
}
