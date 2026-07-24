package com.radion.web;

import com.radion.domain.enums.Platform;
import com.radion.service.pipeline.InformationCollectionEngine;
import com.radion.service.pipeline.models.RawPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final InformationCollectionEngine pipelineEngine;

    @Value("${whatsapp.webhook.verify-token:radion-secure-token}")
    private String verifyToken;

    // WhatsApp Cloud API Verification Endpoint
    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWhatsAppWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp Webhook verified successfully.");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // WhatsApp Cloud API Message Receiver
    @PostMapping("/whatsapp")
    public ResponseEntity<Void> receiveWhatsAppMessage(@RequestBody String rawJsonPayload) {
        log.info("Received WhatsApp Webhook payload");
        
        RawPayload payload = RawPayload.builder()
                .externalMessageId(UUID.randomUUID().toString()) // WhatsApp provides message IDs inside the JSON
                .platform(Platform.WHATSAPP)
                .rawJsonContent(rawJsonPayload)
                .build();

        // Push directly into the existing AI Pipeline
        pipelineEngine.processRawPayload(payload);
        
        return ResponseEntity.ok().build();
    }
}