package com.radion.service.pipeline.placement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.MessageClassification;
import com.radion.domain.enums.MessageProcessingState;
import com.radion.domain.models.AIProcessingLog;
import com.radion.domain.models.Message;
import com.radion.repository.AIProcessingLogRepository;
import com.radion.repository.MessageRepository;
import com.radion.service.pipeline.placement.ai.PlacementExtractionService;
import com.radion.service.pipeline.placement.classification.EmailClassificationEngine;
import com.radion.service.pipeline.placement.dto.PlacementExtractionDTO;
import com.radion.service.pipeline.placement.task.PlacementTaskService;
import com.radion.service.pipeline.placement.timeline.TimelineMatchingService;
import com.radion.service.pipeline.placement.trust.TrustScoringEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementPipelineOrchestrator {

    private final MessageRepository messageRepository;
    private final AIProcessingLogRepository aiProcessingLogRepository;
    private final TrustScoringEngine trustScoringEngine;
    private final EmailClassificationEngine classificationEngine;
    private final PlacementExtractionService extractionService;
    private final TimelineMatchingService timelineService;
    private final PlacementTaskService taskService;
    private final ObjectMapper objectMapper;

    @Value("${radion.pipeline.confidence-threshold:0.70}")
    private double confidenceThreshold;

    /**
     * Executes the Placement Intelligence Pipeline on a raw or synchronized message.
     * Guaranteed never to process the same message twice.
     *
     * @param message The message entity to process.
     */
    public void processMessage(Message message) {
        log.info("Starting Placement Intelligence Pipeline for message ID: {}", message.getId());

        // 1. Deduplication & State Check
        if (message.getProcessingState() != null && 
            message.getProcessingState() != MessageProcessingState.NEW && 
            message.getProcessingState() != MessageProcessingState.FAILED) {
            log.warn("Skipping message {}: Already processed in state {}. Never process the same email twice.", 
                     message.getId(), message.getProcessingState());
            return;
        }

        try {
            message.setProcessingState(MessageProcessingState.NEW);

            // 2. Trust Scoring
            int trustScore = trustScoringEngine.calculateTrustScore(message);
            message.setTrustScore(trustScore);

            // 3. Classification & Spam/Marketing Detection
            MessageClassification classification = classificationEngine.classify(message);
            message.setClassification(classification);

            if (!classificationEngine.isEligibleForExtraction(classification, trustScore)) {
                log.info("Message {} classified as {} (Trust: {}). Stopping pipeline and marking IGNORED.", 
                         message.getId(), classification, trustScore);
                message.setProcessingState(MessageProcessingState.IGNORED);
                messageRepository.save(message);
                return;
            }

            message.setProcessingState(MessageProcessingState.CLASSIFIED);
            messageRepository.save(message);

            // 4. Gemini Placement AI Extraction
            PlacementExtractionDTO extraction = extractionService.extractPlacementData(message);
            message.setProcessingState(MessageProcessingState.AI_PROCESSED);
            messageRepository.save(message);

            // Log AI activity
            try {
                AIProcessingLog logEntry = AIProcessingLog.builder()
                        .message(message)
                        .extractedJson(objectMapper.writeValueAsString(extraction))
                        .aiSummary(extraction.getCompany() + " - " + extraction.getRole() + " (" + extraction.getStage() + ")")
                        .confidenceScore(extraction.getConfidence())
                        .build();
                aiProcessingLogRepository.save(logEntry);
            } catch (Exception e) {
                log.warn("Could not save AIProcessingLog for message {}", message.getId(), e);
            }

            // 5. Confidence Threshold Verification
            if (extraction.getConfidence() < confidenceThreshold) {
                log.warn("Extraction confidence ({}) below threshold ({}). Marking message {} for MANUAL_REVIEW.", 
                         extraction.getConfidence(), confidenceThreshold, message.getId());
                message.setProcessingState(MessageProcessingState.MANUAL_REVIEW);
                messageRepository.save(message);
                return;
            }

            // 6. Timeline Matching & Funnel Management
            timelineService.matchAndUpdateTimeline(message.getUser(), message, extraction);
            message.setProcessingState(MessageProcessingState.TIMELINE_UPDATED);
            messageRepository.save(message);

            // 7. Actionable Task Creation
            var tasks = taskService.createTasksIfActionable(message.getUser(), message, extraction);
            if (!tasks.isEmpty()) {
                message.setProcessingState(MessageProcessingState.TASK_CREATED);
                messageRepository.save(message);
            }

            log.info("Successfully finished Placement Intelligence Pipeline for message {}. Final State: {}", 
                     message.getId(), message.getProcessingState());

        } catch (Exception e) {
            log.error("Pipeline processing failed for message {}", message.getId(), e);
            message.setProcessingState(MessageProcessingState.FAILED);
            try {
                messageRepository.save(message);
            } catch (Exception ex) {
                log.error("Failed to save FAILED state for message {}", message.getId(), ex);
            }
        }
    }
}
