package com.radion.repository;

import com.radion.domain.models.ClassroomAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomAnnouncementRepository extends JpaRepository<ClassroomAnnouncement, UUID> {
    Optional<ClassroomAnnouncement> findByGoogleAnnouncementIdAndUserId(String googleAnnouncementId, UUID userId);

    @Query("SELECT ca FROM ClassroomAnnouncement ca WHERE ca.processingState = 'NEW' ORDER BY ca.updateTime ASC")
    List<ClassroomAnnouncement> findNewAnnouncements(org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT ca FROM ClassroomAnnouncement ca WHERE ca.processingState = 'FAILED' AND (ca.nextRetryAt IS NULL OR ca.nextRetryAt <= :now) ORDER BY ca.nextRetryAt ASC")
    List<ClassroomAnnouncement> findFailedAnnouncementsForRetry(@Param("now") LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT ca FROM ClassroomAnnouncement ca WHERE ca.user.id = :userId ORDER BY ca.updateTime DESC")
    List<ClassroomAnnouncement> findRecentAnnouncements(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);
}
