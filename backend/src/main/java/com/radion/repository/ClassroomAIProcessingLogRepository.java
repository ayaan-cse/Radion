package com.radion.repository;

import com.radion.domain.models.ClassroomAIProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomAIProcessingLogRepository extends JpaRepository<ClassroomAIProcessingLog, UUID> {
    Optional<ClassroomAIProcessingLog> findByCourseWorkId(UUID courseWorkId);
    Optional<ClassroomAIProcessingLog> findByAnnouncementId(UUID announcementId);
}
