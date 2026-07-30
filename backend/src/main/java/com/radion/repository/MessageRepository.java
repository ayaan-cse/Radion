package com.radion.repository;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findTop5ByUserIdOrderByReceivedAtDesc(UUID userId);
    Optional<Message> findByUserIdAndExternalId(UUID userId, String externalId);
    List<Message> findByUserIdAndPlatformOrderByReceivedAtDesc(UUID userId, Platform platform, Pageable pageable);
    List<Message> findByUserIdAndPlatformOrderByReceivedAtDesc(UUID userId, Platform platform);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Message m WHERE m.processingState = com.radion.domain.enums.MessageProcessingState.FAILED AND (m.nextRetryAt IS NULL OR m.nextRetryAt <= :now)")
    List<Message> findMessagesForRetry(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now, Pageable pageable);
}