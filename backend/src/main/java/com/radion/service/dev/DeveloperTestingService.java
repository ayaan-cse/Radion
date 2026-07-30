package com.radion.service.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.CompanyTimeline;
import com.radion.domain.models.Event;
import com.radion.domain.models.Message;
import com.radion.domain.models.Task;
import com.radion.repository.CompanyTimelineRepository;
import com.radion.repository.EventRepository;
import com.radion.repository.MessageRepository;
import com.radion.repository.TaskRepository;
import com.radion.repository.UserRepository;
import com.radion.service.dev.dto.DryRunJourneyReasoningResponse;
import com.radion.service.dev.dto.DryRunJourneyReasoningResponse.EmailReasoningEvaluationResult;
import com.radion.service.dev.dto.DryRunJourneyReasoningResponse.SummaryStats;
import com.radion.service.pipeline.models.JourneyReasoningResponse;
import com.radion.service.pipeline.reasoning.JourneyReasoningAIProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeveloperTestingService {

    private final MessageRepository messageRepository;
    private final JourneyReasoningAIProvider reasoningProvider;
    private final CompanyTimelineRepository timelineRepository;
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final com.radion.service.engine.BusinessCommandExecutor businessCommandExecutor;
    private final ObjectMapper objectMapper;

    /**
     * Executes a read-only dry-run journey reasoning test against stored Gmail messages.
     * Does NOT create timelines, tasks, calendar events, or modify message state in the database.
     *
     * @param userId The ID of the user whose messages should be tested.
     * @param limit The maximum number of messages to evaluate (-1 or <= 0 for All messages).
     * @return DryRunJourneyReasoningResponse containing summary statistics and evaluation results.
     */
    public DryRunJourneyReasoningResponse executeDryRun(UUID userId, int limit) {
        log.info("Starting read-only AI Journey Reasoning Engine dry run for user {} with limit {}", userId, limit);

        List<Message> messages;
        if (limit > 0) {
            messages = messageRepository.findByUserIdAndPlatformOrderByReceivedAtDesc(userId, Platform.GMAIL, PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findByUserIdAndPlatformOrderByReceivedAtDesc(userId, Platform.GMAIL);
        }

        log.info("Fetched {} Gmail messages for dry-run evaluation", messages.size());

        String activeContextJson = loadActiveContext(userId);
        List<EmailReasoningEvaluationResult> results = new ArrayList<>();
        int batchSize = 50;

        // Controlled batching with a bounded thread pool (4 worker threads) to prevent resource exhaustion and rate-limit spikes
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            for (int i = 0; i < messages.size(); i += batchSize) {
                int end = Math.min(messages.size(), i + batchSize);
                List<Message> batch = messages.subList(i, end);
                List<CompletableFuture<EmailReasoningEvaluationResult>> futures = batch.stream()
                        .map(msg -> CompletableFuture.supplyAsync(() -> evaluateMessage(msg, activeContextJson, userId), executor))
                        .collect(Collectors.toList());

                for (int j = 0; j < batch.size(); j++) {
                    Message msg = batch.get(j);
                    CompletableFuture<EmailReasoningEvaluationResult> future = futures.get(j);
                    try {
                        results.add(future.join());
                    } catch (Exception e) {
                        log.error("Error evaluating message in dry run batch: {}", e.getMessage(), e);
                        results.add(EmailReasoningEvaluationResult.builder()
                                .messageId(msg.getId())
                                .sender(msg.getSender() != null ? msg.getSender() : "Unknown Sender")
                                .subject(msg.getTitle() != null ? msg.getTitle() : "No Subject")
                                .receivedAt(msg.getReceivedAt() != null ? msg.getReceivedAt().toString() : "")
                                .hasJourneyImpact(false)
                                .uncertainty(true)
                                .error(true)
                                .emailSummary("Evaluation failed due to exception")
                                .errorMessage(e.getMessage())
                                .commands(new ArrayList<>())
                                .build());
                    }
                }
            }
        } finally {
            executor.shutdown();
        }

        int totalEmails = results.size();
        int impactfulCount = (int) results.stream().filter(r -> r.isHasJourneyImpact() && !r.getCommands().isEmpty()).count();
        int ignoredCount = (int) results.stream().filter(r -> !r.isHasJourneyImpact() || r.getCommands().isEmpty()).count();
        int uncertainCount = (int) results.stream().filter(EmailReasoningEvaluationResult::isUncertainty).count();
        int errorCount = (int) results.stream().filter(EmailReasoningEvaluationResult::isError).count();
        int totalCommandsPredicted = results.stream().mapToInt(r -> r.getCommands().size()).sum();

        SummaryStats stats = SummaryStats.builder()
                .totalEmails(totalEmails)
                .impactfulCount(impactfulCount)
                .ignoredCount(ignoredCount)
                .uncertainCount(uncertainCount)
                .errorCount(errorCount)
                .totalCommandsPredicted(totalCommandsPredicted)
                .build();

        log.info("Completed AI Journey Reasoning dry run: Total={}, Impactful={}, Ignored={}, Uncertain={}, Error={}, TotalCommands={}",
                totalEmails, impactfulCount, ignoredCount, uncertainCount, errorCount, totalCommandsPredicted);

        return DryRunJourneyReasoningResponse.builder()
                .summary(stats)
                .results(results)
                .build();
    }

    private EmailReasoningEvaluationResult evaluateMessage(Message msg, String activeContextJson, UUID userId) {
        long startTime = System.currentTimeMillis();
        JourneyReasoningResponse res = reasoningProvider.reasonOverMessage(msg, activeContextJson);

        com.radion.domain.models.User user = msg.getUser();
        if (user == null && userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (res.getCommands() != null && !res.getCommands().isEmpty() && user != null) {
            try {
                businessCommandExecutor.executeCommands(user, msg, res.getCommands(), true);
            } catch (Exception e) {
                log.warn("Dry-run execution simulation failed for message {}: {}", msg.getId(), e.getMessage());
            }
        }

        return EmailReasoningEvaluationResult.builder()
                .messageId(msg.getId())
                .sender(msg.getSender() != null ? msg.getSender() : "Unknown Sender")
                .subject(msg.getTitle() != null ? msg.getTitle() : "No Subject")
                .receivedAt(msg.getReceivedAt() != null ? msg.getReceivedAt().toString() : "")
                .hasJourneyImpact(res.isHasJourneyImpact())
                .uncertainty(res.isUncertainty())
                .error(res.isError())
                .emailSummary(res.getEmailSummary() != null ? res.getEmailSummary() : "")
                .uncertaintyReason(res.getUncertaintyReason())
                .errorMessage(res.getErrorMessage())
                .processingDurationMs(System.currentTimeMillis() - startTime)
                .commands(res.getCommands() != null ? res.getCommands() : new ArrayList<>())
                .build();
    }

    private String loadActiveContext(UUID userId) {
        if (userId == null) return "{}";
        try {
            List<CompanyTimeline> timelines = timelineRepository.findByUserIdOrderByLastUpdatedDesc(userId);
            List<Task> tasks = taskRepository.findByUserIdAndIsCompletedFalse(userId);
            List<Event> events = eventRepository.findByUserIdAndEventTimeAfterOrderByEventTimeAsc(userId, LocalDateTime.now().minusDays(1));

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
            log.warn("Failed to serialize active context for user {}", userId, e);
            return "{}";
        }
    }
}
