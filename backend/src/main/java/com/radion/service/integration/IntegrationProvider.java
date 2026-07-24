package com.radion.service.integration;

import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;

public interface IntegrationProvider {
    /**
     * Identifies which platform this provider handles.
     */
    Platform getPlatform();

    /**
     * Executes the data synchronization for the given user and connection.
     * Fetches raw messages/payloads and saves them to the MessageRepository.
     */
    void sync(User user, ConnectedService connection);
    
    /**
     * Refreshes the access token if expired.
     */
    boolean refreshTokenIfNeeded(ConnectedService connection);
}