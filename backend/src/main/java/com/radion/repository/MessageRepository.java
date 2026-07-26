package com.radion.repository;
import com.radion.domain.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findTop5ByUserIdOrderByReceivedAtDesc(UUID userId);
    Optional<Message> findByUserIdAndExternalId(UUID userId, String externalId);
}