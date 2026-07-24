package com.radion.service.integration.providers;

import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.service.integration.IntegrationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WhatsAppIntegrationProvider implements IntegrationProvider {

    @Override
    public Platform getPlatform() {
        return Platform.WHATSAPP;
    }

    @Override
    public void sync(User user, ConnectedService connection) {
        log.info("Starting WhatsApp sync for user: {}", user.getId());
        // TODO: Implement WhatsApp Cloud API / Webhook polling retrieval
        log.info("WhatsApp sync completed for user: {}", user.getId());
    }

    @Override
    public boolean refreshTokenIfNeeded(ConnectedService connection) {
        // WhatsApp Cloud API typically uses long-lived system user tokens or webhooks,
        // so standard OAuth refresh might not apply here.
        return true; 
    }
}