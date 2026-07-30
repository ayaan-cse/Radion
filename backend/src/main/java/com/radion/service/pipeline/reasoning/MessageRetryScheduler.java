package com.radion.service.pipeline.reasoning;

import com.radion.domain.models.Message;
import com.radion.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MessageRetryScheduler {

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("MessageRetryScheduler bean instantiated and initialized!");
    }

    private final MessageRepository messageRepository;
    private final JourneyPipelineOrchestrator orchestrator;

    public MessageRetryScheduler(MessageRepository messageRepository, JourneyPipelineOrchestrator orchestrator) {
        this.messageRepository = messageRepository;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processRetryQueue() {
        log.info("MessageRetryScheduler is running...");
        LocalDateTime now = LocalDateTime.now();
        List<Message> failedMessages = messageRepository.findMessagesForRetry(now, PageRequest.of(0, 10));

        if (failedMessages.isEmpty()) {
            return;
        }

        log.info("Found {} failed messages to retry.", failedMessages.size());

        for (Message message : failedMessages) {
            log.info("Retrying message ID: {} (Retry Count: {})", message.getId(), message.getRetryCount());
            
            // Process the message via orchestrator
            orchestrator.processMessage(message);
            // The orchestrator sets the processing state.
            // If it succeeds, the state will be TIMELINE_UPDATED, AI_PROCESSED, etc.
            // If it fails again, the orchestrator will set it back to FAILED.
        }
    }
}
