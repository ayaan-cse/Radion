package com.radion.repository;
import com.radion.domain.models.AIProcessingLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AIProcessingLogRepository extends JpaRepository<AIProcessingLog, UUID> {
    Optional<AIProcessingLog> findByMessageId(UUID messageId);

    @Query("SELECT l FROM AIProcessingLog l " +
           "WHERE l.message.user.id = :userId " +
           "AND l.message.processingState NOT IN ('NEW', 'CLASSIFIED') " +
           "ORDER BY l.message.receivedAt DESC")
    List<AIProcessingLog> findRecentPlacementLogs(@Param("userId") UUID userId, Pageable pageable);
}