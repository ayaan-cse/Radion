package com.radion.service.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radion.domain.enums.BusinessCommandType;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.Message;
import com.radion.repository.CompanyTimelineRepository;
import com.radion.repository.EventRepository;
import com.radion.repository.MessageRepository;
import com.radion.repository.TaskRepository;
import com.radion.service.dev.dto.DryRunJourneyReasoningResponse;
import com.radion.service.pipeline.models.BusinessCommand;
import com.radion.service.pipeline.models.JourneyReasoningResponse;
import com.radion.service.pipeline.reasoning.JourneyReasoningAIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeveloperTestingServiceTest {

    private MessageRepository mockMessageRepository;
    private JourneyReasoningAIProvider mockReasoningProvider;
    private CompanyTimelineRepository mockTimelineRepository;
    private TaskRepository mockTaskRepository;
    private EventRepository mockEventRepository;
    private ObjectMapper objectMapper;
    private DeveloperTestingService developerTestingService;

    @BeforeEach
    void setUp() {
        mockMessageRepository = Mockito.mock(MessageRepository.class);
        mockReasoningProvider = Mockito.mock(JourneyReasoningAIProvider.class);
        mockTimelineRepository = Mockito.mock(CompanyTimelineRepository.class);
        mockTaskRepository = Mockito.mock(TaskRepository.class);
        mockEventRepository = Mockito.mock(EventRepository.class);
        objectMapper = new ObjectMapper();
        com.radion.repository.UserRepository mockUserRepository = Mockito.mock(com.radion.repository.UserRepository.class);
        com.radion.service.engine.BusinessCommandExecutor mockExecutor = Mockito.mock(com.radion.service.engine.BusinessCommandExecutor.class);
        developerTestingService = new DeveloperTestingService(
                mockMessageRepository, mockReasoningProvider, mockTimelineRepository, mockTaskRepository, mockEventRepository, mockUserRepository, mockExecutor, objectMapper);
    }

    @Test
    void testDryRunStatsAggregationAndNoSideEffects() {
        UUID userId = UUID.randomUUID();

        Message processMsg = Message.builder()
                .id(UUID.randomUUID())
                .sender("hr@google.com")
                .title("Job Offer: Software Engineer")
                .snippet("We are thrilled to offer you the position.")
                .build();

        Message ignoreMsg = Message.builder()
                .id(UUID.randomUUID())
                .sender("scammer@fake.com")
                .title("100% placement guarantee course rs 500")
                .snippet("Enroll in our guaranteed internship.")
                .build();

        List<Message> mockMessages = List.of(processMsg, ignoreMsg);
        when(mockMessageRepository.findByUserIdAndPlatformOrderByReceivedAtDesc(eq(userId), eq(Platform.GMAIL), any(PageRequest.class)))
                .thenReturn(mockMessages);

        when(mockReasoningProvider.reasonOverMessage(eq(processMsg), anyString()))
                .thenReturn(JourneyReasoningResponse.builder()
                        .hasJourneyImpact(true)
                        .emailSummary("Offer received from Google")
                        .commands(List.of(BusinessCommand.builder().commandType(BusinessCommandType.ADVANCE_OPPORTUNITY_STAGE).stage("OFFER").build()))
                        .build());

        when(mockReasoningProvider.reasonOverMessage(eq(ignoreMsg), anyString()))
                .thenReturn(JourneyReasoningResponse.builder()
                        .hasJourneyImpact(false)
                        .emailSummary("Spam promotional email")
                        .commands(List.of())
                        .build());

        DryRunJourneyReasoningResponse response = developerTestingService.executeDryRun(userId, 25);

        assertNotNull(response);
        assertNotNull(response.getSummary());
        assertEquals(2, response.getSummary().getTotalEmails());
        assertEquals(1, response.getSummary().getImpactfulCount());
        assertEquals(1, response.getSummary().getIgnoredCount());
        assertEquals(1, response.getSummary().getTotalCommandsPredicted());

        assertEquals(2, response.getResults().size());
        
        DryRunJourneyReasoningResponse.EmailReasoningEvaluationResult res0 = response.getResults().get(0);
        assertEquals(processMsg.getId(), res0.getMessageId());
        assertEquals("hr@google.com", res0.getSender());
        assertTrue(res0.isHasJourneyImpact());
        assertEquals("Offer received from Google", res0.getEmailSummary());

        // Verify ZERO database side effects (read-only guarantee)
        verify(mockMessageRepository, never()).save(any());
        verify(mockMessageRepository, never()).saveAll(any());
        verify(mockMessageRepository, never()).delete(any());
        verify(mockMessageRepository, never()).deleteAll();
    }

    @Test
    void testDryRunAllMessagesLimit() {
        UUID userId = UUID.randomUUID();
        when(mockMessageRepository.findByUserIdAndPlatformOrderByReceivedAtDesc(eq(userId), eq(Platform.GMAIL)))
                .thenReturn(List.of());

        DryRunJourneyReasoningResponse response = developerTestingService.executeDryRun(userId, -1);

        assertNotNull(response);
        assertEquals(0, response.getSummary().getTotalEmails());
        assertTrue(response.getResults().isEmpty());
        verify(mockMessageRepository, times(1)).findByUserIdAndPlatformOrderByReceivedAtDesc(eq(userId), eq(Platform.GMAIL));
        verify(mockMessageRepository, never()).save(any());
    }
}
