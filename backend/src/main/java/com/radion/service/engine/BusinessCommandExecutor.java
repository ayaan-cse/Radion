package com.radion.service.engine;

import com.radion.domain.models.*;
import com.radion.service.calendar.GoogleCalendarSyncService;
import com.radion.service.engine.dto.CommandExecutionReport;
import com.radion.service.engine.dto.PendingCalendarSync;
import com.radion.service.pipeline.models.BusinessCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes semantic Business Commands emitted by Gemini against the PostgreSQL database.
 * Enforces strict separation of concerns: AI decides WHAT happened; this executor decides HOW it is validated, deduplicated, and persisted.
 * Uses a Split-Phase Transaction architecture to separate local DB writes from external network calls.
 */
@Slf4j
@Service
public class BusinessCommandExecutor {

    private final BusinessCommandLocalPersister localPersister;
    private final GoogleCalendarSyncService calendarSyncService;
    private final EventEngine eventEngine;

    @Autowired
    public BusinessCommandExecutor(
            BusinessCommandLocalPersister localPersister,
            EventEngine eventEngine,
            @Autowired(required = false) GoogleCalendarSyncService calendarSyncService) {
        this.localPersister = localPersister;
        this.eventEngine = eventEngine;
        this.calendarSyncService = calendarSyncService;
    }

    /**
     * Executes a list of Business Commands using a split-phase approach.
     */
    public List<CommandExecutionReport> executeCommands(User user, Message sourceMessage, List<BusinessCommand> commands, boolean isDryRun) {
        List<CommandExecutionReport> reports = new ArrayList<>();
        if (commands == null || commands.isEmpty()) {
            log.info("BusinessCommandExecutor received empty or null command list for message ID: {}",
                     sourceMessage != null ? sourceMessage.getId() : "null");
            return reports;
        }

        log.info("Executing {} business commands for user {} (dryRun={})", commands.size(), user != null ? user.getId() : "null", isDryRun);
        for (BusinessCommand command : commands) {
            CommandExecutionReport report = executeCommand(user, sourceMessage, command, isDryRun);
            reports.add(report);
        }
        return reports;
    }

    /**
     * Executes a single Business Command strictly orchestrating the split phases.
     * Transactional boundaries are handled by localPersister.
     */
    public CommandExecutionReport executeCommand(User user, Message sourceMessage, BusinessCommand command, boolean isDryRun) {
        CommandExecutionReport report = CommandExecutionReport.builder()
                .rawCommand(command)
                .valid(true)
                .validationErrors(new ArrayList<>())
                .executed(false)
                .build();

        if (command == null || command.getCommandType() == null) {
            report.setValid(false);
            report.getValidationErrors().add("Command or CommandType is null");
            report.setErrorMessage("Invalid command structure");
            log.warn("Validation failed: Command or CommandType is null");
            if (command != null) populateCommandReportFields(command, report);
            return report;
        }

        if (user == null) {
            report.setValid(false);
            report.getValidationErrors().add("User target is required for command execution");
            report.setErrorMessage("Target User is null");
            log.warn("Validation failed: User is null for command [{}]", command.getCommandType());
            populateCommandReportFields(command, report);
            return report;
        }

        PendingCalendarSync pendingSync = null;

        // Phase 1: Atomic Local DB Prep
        try {
            pendingSync = localPersister.persistCommandWorldState(user, sourceMessage, command, isDryRun, report);
        } catch (Exception e) {
            log.error("Execution failure for command [{}] on message ID {}: {}",
                      command.getCommandType(), sourceMessage != null ? sourceMessage.getId() : "null", e.getMessage(), e);
            report.setExecuted(false);
            report.setErrorMessage("Execution Exception: " + e.getMessage());
            if (!isDryRun) {
                populateCommandReportFields(command, report);
                throw new RuntimeException("Business command execution failed during Phase 1: " + e.getMessage(), e);
            }
        }

        // Phase 2 & 3: Network I/O and Atomic Local Update
        if (!isDryRun && pendingSync != null && pendingSync.getEvent() != null) {
            if (calendarSyncService != null) {
                String error = null;
                String gCalId = null;
                String status = "FAILED";
                Exception syncException = null;
                try {
                    if (pendingSync.isUpdate() && pendingSync.getEvent().getGoogleCalendarEventId() != null) {
                        gCalId = calendarSyncService.updateEvent(user, pendingSync.getEvent().getGoogleCalendarEventId(), pendingSync.getCalendarDTO());
                    } else {
                        gCalId = calendarSyncService.syncEvent(user, pendingSync.getCalendarDTO());
                    }
                    if (gCalId != null) {
                        status = "SYNCED";
                        report.setCalendarSyncResult(status + ": Google Calendar Event ID " + gCalId);
                        report.setCalendarEventId(gCalId);
                        log.info("Successfully synced Event [{}] to Google Calendar Event ID {}", pendingSync.getEvent().getId(), gCalId);
                    }
                } catch (Exception e) {
                    syncException = e;
                    log.warn("Google Calendar sync failed for event {}: {}. Database transaction will NOT be rolled back.", pendingSync.getEvent().getId(), e.getMessage());
                    report.setCalendarSyncResult("FAILED: " + e.getMessage());
                }
                
                // Phase 3 Update
                eventEngine.updateCalendarSyncStatus(pendingSync.getEvent().getId(), gCalId, status, syncException);
                
            } else {
                report.setCalendarSyncResult("NOT_APPLICABLE: Calendar sync service not active");
                eventEngine.updateCalendarSyncStatus(pendingSync.getEvent().getId(), null, "FAILED", new RuntimeException("Calendar sync service not active"));
            }
        }

        populateCommandReportFields(command, report);
        log.info("Command [{}] execution report: valid={}, executed={}, plan='{}', result='{}', error='{}'",
                 command.getCommandType(), report.isValid(), report.isExecuted(),
                 report.getExecutionPlan(), report.getExecutionResult(), report.getErrorMessage());
        return report;
    }

    private void populateCommandReportFields(BusinessCommand command, CommandExecutionReport report) {
        command.setValid(report.isValid());
        command.setValidationErrors(report.getValidationErrors());
        command.setExecutionPlan(report.getExecutionPlan());
        command.setExecutionResult(report.getExecutionResult());
        command.setExecutionError(report.getErrorMessage());
        command.setCalendarSyncResult(report.getCalendarSyncResult());
        command.setCalendarEventId(report.getCalendarEventId());
    }
}
