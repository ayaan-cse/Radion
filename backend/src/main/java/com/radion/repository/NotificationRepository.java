package com.radion.repository;
import com.radion.domain.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    int countByUserIdAndIsReadFalse(UUID userId);
}