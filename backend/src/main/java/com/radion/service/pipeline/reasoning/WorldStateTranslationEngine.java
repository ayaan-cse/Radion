package com.radion.service.pipeline.reasoning;

import com.radion.domain.models.Message;
import com.radion.domain.models.User;
import com.radion.service.engine.BusinessCommandExecutor;
import com.radion.service.engine.dto.CommandExecutionReport;
import com.radion.service.pipeline.models.BusinessCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Facade that bridges AI reasoning outputs to the BusinessCommandExecutor service.
 * Translates semantic Business Commands (emitted by the AI Reasoning Engine) into transactional
 * database operations against the student's evolving world state (Opportunities, Tasks, Events).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorldStateTranslationEngine {

    private final BusinessCommandExecutor businessCommandExecutor;

    public int executeCommands(User user, Message sourceMessage, List<BusinessCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            log.info("No business commands to execute for message ID: {}", 
                     sourceMessage != null ? sourceMessage.getId() : "null");
            return 0;
        }

        List<CommandExecutionReport> reports = businessCommandExecutor.executeCommands(user, sourceMessage, commands, false);
        int successCount = (int) reports.stream().filter(CommandExecutionReport::isExecuted).count();
        log.info("Successfully executed {}/{} business commands via BusinessCommandExecutor for message ID: {}", 
                 successCount, commands.size(), sourceMessage != null ? sourceMessage.getId() : "null");
        return successCount;
    }
}
