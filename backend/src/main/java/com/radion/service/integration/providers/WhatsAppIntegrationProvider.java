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
    public int sync(User user, ConnectedService connection) {
        log.info("WhatsApp sync is not implemented yet for user: {}", user.getId());
        return 0;
    }

    @Override
    public boolean refreshTokenIfNeeded(ConnectedService connection) {
        // WhatsApp Cloud API typically uses long-lived system user tokens or webhooks,
        // so standard OAuth refresh might not apply here.
        return true; 
    }
}