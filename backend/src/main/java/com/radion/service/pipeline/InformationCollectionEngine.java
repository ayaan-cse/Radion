package com.radion.service.pipeline;

import com.radion.domain.enums.Platform;
import com.radion.service.pipeline.ai.AIIntelligenceEngine;
import com.radion.service.pipeline.automation.AutomationEngine;
import com.radion.service.pipeline.models.AIExtractionResult;
import com.radion.service.pipeline.models.NormalizedMessage;
import com.radion.service.pipeline.models.RawPayload;
import com.radion.service.pipeline.parser.MessageParser;
import com.radion.service.pipeline.validation.PipelineValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InformationCollectionEngine {

    private final Map<Platform, MessageParser> parsers;
    private final AIIntelligenceEngine aiEngine;
    private final PipelineValidator validator;
    private final AutomationEngine automationEngine;

    public InformationCollectionEngine(
            List<MessageParser> parserList,
            AIIntelligenceEngine aiEngine,
            PipelineValidator validator,
            AutomationEngine automationEngine) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(MessageParser::getSupportedPlatform, Function.identity()));
        this.aiEngine = aiEngine;
        this.validator = validator;
        this.automationEngine = automationEngine;
    }

    /**
     * Entry point for raw data coming from Gmail/WhatsApp/Classroom integrations.
     */
    public void processRawPayload(RawPayload payload) {
        log.info("Received raw payload from platform: {}", payload.getPlatform());

        // 1. Parse & Normalize
        MessageParser parser = parsers.get(payload.getPlatform());
        if (parser == null) {
            log.error("No parser found for platform: {}", payload.getPlatform());
            return;
        }
        NormalizedMessage normalizedMessage = parser.parse(payload);

        // 2. AI Extraction
        AIExtractionResult extractionResult = aiEngine.extractInformation(normalizedMessage);

        // 3. Validation & Duplicate Detection
        if (!validator.isValid(extractionResult)) {
            log.info("Extraction validation failed. Dropping payload.");
            return;
        }
        if (validator.isDuplicate(normalizedMessage, extractionResult)) {
            log.info("Duplicate event detected. Dropping payload.");
            return;
        }

        // 4. Automation Decision
        AutomationEngine.ActionDecision decision = automationEngine.decideAction(extractionResult);
        automationEngine.executeDecision(decision, normalizedMessage, extractionResult);
        
        // Note: AIProcessingLog entity creation will happen here to populate the 
        // "Recent AI Processed Messages" card on the frontend.
    }
}