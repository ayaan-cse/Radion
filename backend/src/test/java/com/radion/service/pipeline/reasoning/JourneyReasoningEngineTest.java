package com.radion.service.pipeline.reasoning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.*;
import com.radion.domain.models.*;
import com.radion.repository.*;
import com.radion.service.pipeline.models.*;
import com.radion.service.pipeline.parser.MessageParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JourneyReasoningEngineTest {

    private MessageRepository messageRepo;
    private AIProcessingLogRepository logRepo;
    private CompanyTimelineRepository timelineRepo;
    private TaskRepository taskRepo;
    private EventRepository eventRepo;
    private UserRepository userRepo;
    private JourneyReasoningAIProvider reasoningProvider;
    private WorldStateTranslationEngine translationEngine;
    private JourneyPipelineOrchestrator orchestrator;
    private ObjectMapper objectMapper;

    private User testUser;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        messageRepo = Mockito.mock(MessageRepository.class);
        logRepo = Mockito.mock(AIProcessingLogRepository.class);
        timelineRepo = Mockito.mock(CompanyTimelineRepository.class);
        taskRepo = Mockito.mock(TaskRepository.class);
        eventRepo = Mockito.mock(EventRepository.class);
        userRepo = Mockito.mock(UserRepository.class);
        reasoningProvider = Mockito.mock(JourneyReasoningAIProvider.class);
        objectMapper = new ObjectMapper();

        com.radion.service.engine.BusinessCommandLocalPersister localPersister = new com.radion.service.engine.BusinessCommandLocalPersister(timelineRepo, taskRepo, eventRepo);
        com.radion.service.engine.EventEngine eventEngine = Mockito.mock(com.radion.service.engine.EventEngine.class);
        com.radion.service.engine.BusinessCommandExecutor executor = new com.radion.service.engine.BusinessCommandExecutor(localPersister, eventEngine, null);
        translationEngine = new WorldStateTranslationEngine(executor);
        orchestrator = new JourneyPipelineOrchestrator(
                messageRepo, logRepo, timelineRepo, taskRepo, eventRepo, userRepo, 
                reasoningProvider, translationEngine, objectMapper, new ArrayList<MessageParser>());

        testUser = User.builder().id(UUID.randomUUID()).email("student@university.edu").firstName("Test").lastName("Student").build();
        testMessage = Message.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .platform(Platform.GMAIL)
                .sender("recruiter@google.com")
                .title("Interview Invitation: Google SWE Internship")
                .snippet("Hi, we reviewed your resume and would like to schedule a technical interview next week.")
                .receivedAt(LocalDateTime.now())
                .processingState(MessageProcessingState.NEW)
                .build();
    }

    @Test
    void testOrchestratorContextLoadingAndExecution() {
        // Mock context loading
        when(timelineRepo.findByUserIdOrderByLastUpdatedDesc(eq(testUser.getId())))
                .thenReturn(List.of(CompanyTimeline.builder().id(UUID.randomUUID()).companyName("Microsoft").currentStage(TimelineStage.REGISTRATION).build()));
        when(taskRepo.findByUserIdAndIsCompletedFalse(eq(testUser.getId())))
                .thenReturn(List.of(Task.builder().id(UUID.randomUUID()).title("Submit Resume to Amazon").build()));
        when(eventRepo.findByUserIdAndEventTimeAfterOrderByEventTimeAsc(eq(testUser.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Mock AI Reasoning Response with BusinessCommand
        BusinessCommand command = BusinessCommand.builder()
                .commandType(BusinessCommandType.REGISTER_OPPORTUNITY)
                .companyName("Google")
                .stage("TECHNICAL")
                .role("SWE Internship")
                .evidenceQuote("schedule a technical interview next week")
                .build();

        JourneyReasoningResponse aiResponse = JourneyReasoningResponse.builder()
                .hasJourneyImpact(true)
                .emailSummary("Invitation for technical interview at Google")
                .commands(List.of(command))
                .build();

        when(reasoningProvider.reasonOverMessage(eq(testMessage), anyString())).thenReturn(aiResponse);

        // Execute orchestrator
        orchestrator.processMessage(testMessage);

        // Verify context was loaded and passed to AI
        verify(reasoningProvider, times(1)).reasonOverMessage(eq(testMessage), argThat(ctx -> 
            ctx.contains("Microsoft") && ctx.contains("Submit Resume to Amazon")));

        // Verify command was translated and timeline saved
        ArgumentCaptor<CompanyTimeline> timelineCaptor = ArgumentCaptor.forClass(CompanyTimeline.class);
        verify(timelineRepo, times(1)).save(timelineCaptor.capture());
        CompanyTimeline saved = timelineCaptor.getValue();
        assertEquals("Google", saved.getCompanyName());
        assertEquals(TimelineStage.TECHNICAL, saved.getCurrentStage());
        assertEquals("SWE Internship", saved.getRole());

        // Verify message state, summary and command count updated
        assertEquals("Invitation for technical interview at Google", testMessage.getReasoningSummary());
        assertEquals(1, testMessage.getMutationCount());
        assertEquals(MessageProcessingState.TIMELINE_UPDATED, testMessage.getProcessingState());
        verify(messageRepo, times(2)).save(testMessage);
    }

    @Test
    void testNoImpactMessageSkipsCommands() {
        JourneyReasoningResponse aiResponse = JourneyReasoningResponse.builder()
                .hasJourneyImpact(false)
                .emailSummary("Newsletter about tech trends")
                .commands(List.of())
                .build();

        when(reasoningProvider.reasonOverMessage(eq(testMessage), anyString())).thenReturn(aiResponse);

        orchestrator.processMessage(testMessage);

        // Verify NO repository save calls occurred for entity repositories
        verify(timelineRepo, never()).save(any());
        verify(taskRepo, never()).save(any());
        verify(eventRepo, never()).save(any());
        assertEquals("Newsletter about tech trends", testMessage.getReasoningSummary());
        assertEquals(0, testMessage.getMutationCount());
        assertEquals(MessageProcessingState.IGNORED, testMessage.getProcessingState());
        verify(messageRepo, times(2)).save(testMessage);
    }

    @Test
    void testTranslationEngineActionItemCommands() {
        // Test COMPLETE_ACTION_ITEM command
        Task existingTask = Task.builder().id(UUID.randomUUID()).title("Submit assignment").isCompleted(false).build();
        when(taskRepo.findByUserIdAndTitleIgnoreCase(testUser.getId(), "Submit assignment"))
                .thenReturn(List.of(existingTask));

        BusinessCommand completeCmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.COMPLETE_ACTION_ITEM)
                .title("Submit assignment")
                .build();

        int executedCount = translationEngine.executeCommands(testUser, testMessage, List.of(completeCmd));
        assertEquals(1, executedCount);
        assertTrue(existingTask.isCompleted());
        verify(taskRepo, times(1)).save(existingTask);

        // Test ASSIGN_ACTION_ITEM command
        BusinessCommand assignCmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.ASSIGN_ACTION_ITEM)
                .title("Prepare for technical round")
                .dueDate("2026-08-01T10:00:00")
                .build();

        when(taskRepo.existsByUserIdAndTitleIgnoreCase(testUser.getId(), "Prepare for technical round")).thenReturn(false);

        int executedAssignCount = translationEngine.executeCommands(testUser, testMessage, List.of(assignCmd));
        assertEquals(1, executedAssignCount);
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepo, times(2)).save(taskCaptor.capture()); // Second save invocation
        Task createdTask = taskCaptor.getValue();
        assertEquals("Prepare for technical round", createdTask.getTitle());
        assertFalse(createdTask.isCompleted());
    }
}
