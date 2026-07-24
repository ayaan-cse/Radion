package com.radion.repository;
import com.radion.domain.models.AIProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AIProcessingLogRepository extends JpaRepository<AIProcessingLog, UUID> {
    List<AIProcessingLog> findTop5ByMessageUserIdOrderByProcessedAtDesc(UUID userId);
}