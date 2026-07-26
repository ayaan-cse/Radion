package com.radion.repository;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectedServiceRepository extends JpaRepository<ConnectedService, UUID> {
    List<ConnectedService> findByUserId(UUID userId);
    Optional<ConnectedService> findByUserIdAndPlatform(UUID userId, Platform platform);
}