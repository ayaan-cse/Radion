package com.radion.service.integration;

import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.repository.ConnectedServiceRepository;
import com.radion.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncManagerService {

    private final UserRepository userRepository;
    private final ConnectedServiceRepository connectedServiceRepository;
    private final Map<Platform, IntegrationProvider> providerMap;

    // Spring automatically injects all implementations of IntegrationProvider
    public SyncManagerService(
            UserRepository userRepository,
            ConnectedServiceRepository connectedServiceRepository,
            List<IntegrationProvider> providers) {

        this.userRepository = userRepository;
        this.connectedServiceRepository = connectedServiceRepository;

        // Map providers by their platform enum for O(1) lookup
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        IntegrationProvider::getPlatform,
                        Function.identity()
                ));
    }

    /**
     * Unified Sync Execution
     */
    public void executeSync(UUID userId) {
        log.info("Sync execution started for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ConnectedService> connections =
                connectedServiceRepository.findByUserId(userId);
                
        log.info("Found {} connected integrations for userId: {}", connections.size(), userId);

        for (ConnectedService connection : connections) {
            IntegrationProvider provider =
                    providerMap.get(connection.getPlatform());

            if (provider != null) {
                try {
                    log.info("Sync started for platform {} and userId: {}", connection.getPlatform(), userId);
                    int itemsFetched = provider.sync(user, connection);

                    // Unconditionally update lastSyncAt IF the pipeline completed without exceptions
                    connection.setLastSyncAt(LocalDateTime.now());
                    connectedServiceRepository.save(connection);
                    log.info("Updated lastSyncAt for platform {} after fetching {} items.", connection.getPlatform(), itemsFetched);

                    log.info("Sync completed for platform {} and userId: {}", connection.getPlatform(), userId);

                } catch (Exception e) {
                    log.error(
                            "Sync failed for platform {} and user {}. Cursor (lastSyncAt) will NOT be advanced.",
                            connection.getPlatform(),
                            userId,
                            e
                    );
                }
            }
        }
        
        log.info("Sync execution finished for userId: {}", userId);
    }

    /**
     * Automated Background Sync
     * Runs every 15 minutes (900000 ms) by default.
     * Configurable through: radion.sync.interval
     */
    @Scheduled(fixedDelayString = "${radion.sync.interval:900000}")
    public void automatedBackgroundSync() {

        log.info("Starting automated background sync for all active users...");

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                executeSync(user.getId());
            } catch (Exception e) {
                log.error(
                        "Automated sync failed for user: {}",
                        user.getId(),
                        e
                );
            }
        }

        log.info("Automated background sync completed.");
    }
}