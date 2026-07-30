package com.radion.service.engine;

import com.radion.domain.enums.BusinessCommandType;
import com.radion.domain.enums.EventCategory;
import com.radion.domain.enums.TimelineStage;
import com.radion.domain.models.*;
import com.radion.repository.CompanyTimelineRepository;
import com.radion.repository.EventRepository;
import com.radion.repository.TaskRepository;
import com.radion.service.engine.dto.CommandExecutionReport;
import com.radion.service.pipeline.models.BusinessCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessCommandExecutorTest {

    @Mock private CompanyTimelineRepository timelineRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private EventRepository eventRepository;
    @Mock private com.radion.service.calendar.GoogleCalendarSyncService calendarSyncService;

    @InjectMocks
    private BusinessCommandExecutor executor;

    private UUID userId;
    private User testUser;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder().id(userId).firstName("Test Student").build();
        testMessage = Message.builder().id(UUID.randomUUID()).user(testUser).sender("Google Campus Recruitment").title("Interview Invite").build();
    }

    @Test
    void executeCommand_RegisterOpportunity_New_Success() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.REGISTER_OPPORTUNITY)
                .companyName("Google")
                .role("Software Engineer")
                .stage("REGISTRATION")
                .build();

        when(timelineRepository.findByUserIdAndCompanyNameIgnoreCase(userId, "Google")).thenReturn(Optional.empty());
        when(timelineRepository.save(any(CompanyTimeline.class))).thenAnswer(inv -> {
            CompanyTimeline t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        assertNotNull(report.getExecutionPlan());
        assertNull(report.getErrorMessage());
        verify(timelineRepository, times(1)).save(any(CompanyTimeline.class));
    }

    @Test
    void executeCommand_RegisterOpportunity_Duplicate_Skipped() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.REGISTER_OPPORTUNITY)
                .companyName("Google")
                .build();

        CompanyTimeline existing = CompanyTimeline.builder().id(UUID.randomUUID()).companyName("Google").build();
        when(timelineRepository.findByUserIdAndCompanyNameIgnoreCase(userId, "Google")).thenReturn(Optional.of(existing));

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertFalse(report.isExecuted());
        assertTrue(report.getExecutionResult().contains("Skipped duplicate"));
        verify(timelineRepository, never()).save(any());
    }

    @Test
    void executeCommand_AdvanceStage_Existing_Success() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.ADVANCE_OPPORTUNITY_STAGE)
                .companyName("Google")
                .stage("TECHNICAL")
                .build();

        CompanyTimeline existing = CompanyTimeline.builder().id(UUID.randomUUID()).companyName("Google").currentStage(TimelineStage.ASSESSMENT).build();
        when(timelineRepository.findByUserIdAndCompanyNameIgnoreCase(userId, "Google")).thenReturn(Optional.of(existing));
        when(timelineRepository.save(any(CompanyTimeline.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        assertEquals(TimelineStage.TECHNICAL, existing.getCurrentStage());
        verify(timelineRepository, times(1)).save(existing);
    }

    @Test
    void executeCommand_AssignActionItem_New_Success() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.ASSIGN_ACTION_ITEM)
                .title("Submit Resume by Friday")
                .dueDate("2026-07-05")
                .build();

        when(taskRepository.existsByUserIdAndTitleIgnoreCase(userId, "Submit Resume by Friday")).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void executeCommand_AssignActionItem_Duplicate_Skipped() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.ASSIGN_ACTION_ITEM)
                .title("Submit Resume by Friday")
                .build();

        when(taskRepository.existsByUserIdAndTitleIgnoreCase(userId, "Submit Resume by Friday")).thenReturn(true);

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertFalse(report.isExecuted());
        assertTrue(report.getExecutionResult().contains("Skipped duplicate"));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void executeCommand_CompleteActionItem_Existing_Success() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.COMPLETE_ACTION_ITEM)
                .title("Submit Resume by Friday")
                .build();

        Task existingTask = Task.builder().id(UUID.randomUUID()).title("Submit Resume by Friday").isCompleted(false).build();
        when(taskRepository.findByUserIdAndTitleIgnoreCase(userId, "Submit Resume by Friday")).thenReturn(Collections.singletonList(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        assertTrue(existingTask.isCompleted());
        verify(taskRepository, times(1)).save(existingTask);
    }

    @Test
    void executeCommand_ScheduleInterview_Success() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.SCHEDULE_INTERVIEW)
                .title("Technical Interview Round 1")
                .companyName("Google")
                .scheduledTime("2026-07-10T14:00:00")
                .build();

        when(eventRepository.findByUserIdAndCompanyOrSourceAndEventTime(eq(userId), eq("Google"), any(LocalDateTime.class))).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(1)).save(captor.capture());
        assertEquals(EventCategory.INTERVIEW, captor.getValue().getCategory());
        assertEquals("Technical Interview Round 1", captor.getValue().getTitle());
    }

    @Test
    void executeCommand_ValidationFailure_NullCommandType() {
        BusinessCommand cmd = BusinessCommand.builder().companyName("Google").build();

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertFalse(report.isValid());
        assertFalse(report.isExecuted());
        assertFalse(report.getValidationErrors().isEmpty());
    }

    @Test
    void executeCommand_DryRun_NoDatabaseSave() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.REGISTER_OPPORTUNITY)
                .companyName("Microsoft")
                .build();

        when(timelineRepository.findByUserIdAndCompanyNameIgnoreCase(userId, "Microsoft")).thenReturn(Optional.empty());

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, true);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        assertTrue(report.getExecutionResult().contains("Dry-Run"));
        verify(timelineRepository, never()).save(any());
        verify(taskRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void executeCommand_ScheduleInterview_CalendarSyncSuccess() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.SCHEDULE_INTERVIEW)
                .companyName("Google")
                .title("Technical Interview")
                .scheduledTime("2026-08-15T14:00")
                .build();

        when(eventRepository.findByUserIdAndCompanyOrSourceAndEventTime(any(), any(), any())).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(calendarSyncService.syncEvent(any(), any())).thenReturn("gcal-event-id-123");

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        assertEquals("SYNCED: Created Google Calendar Event ID gcal-event-id-123", report.getCalendarSyncResult());
        assertEquals("gcal-event-id-123", report.getCalendarEventId());
        verify(calendarSyncService, times(1)).syncEvent(any(), any());
        verify(eventRepository, times(2)).save(any(Event.class));
    }

    @Test
    void executeCommand_ScheduleInterview_CalendarSyncFailure_NoRollback() {
        BusinessCommand cmd = BusinessCommand.builder()
                .commandType(BusinessCommandType.SCHEDULE_INTERVIEW)
                .companyName("Google")
                .title("Technical Interview")
                .scheduledTime("2026-08-15T14:00")
                .build();

        when(eventRepository.findByUserIdAndCompanyOrSourceAndEventTime(any(), any(), any())).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(calendarSyncService.syncEvent(any(), any())).thenThrow(new RuntimeException("Google Calendar API Error: 500"));

        CommandExecutionReport report = executor.executeCommand(testUser, testMessage, cmd, false);

        assertTrue(report.isValid());
        assertTrue(report.isExecuted());
        assertTrue(report.getCalendarSyncResult().contains("FAILED"));
        verify(eventRepository, times(1)).save(any(Event.class));
    }
}
