package com.radion.repository;

import com.radion.domain.models.ClassroomCourseWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomCourseWorkRepository extends JpaRepository<ClassroomCourseWork, UUID> {
    Optional<ClassroomCourseWork> findByGoogleCourseWorkIdAndUserId(String googleCourseWorkId, UUID userId);

    @Query("SELECT cw FROM ClassroomCourseWork cw WHERE cw.processingState = 'NEW' ORDER BY cw.updateTime ASC")
    List<ClassroomCourseWork> findNewCourseWork(org.springframework.data.domain.Pageable pageable);
    
    @Query("SELECT cw FROM ClassroomCourseWork cw WHERE cw.user.id = :userId AND cw.dueDate >= :now AND cw.processingState = 'AI_PROCESSED' ORDER BY cw.dueDate ASC")
    List<ClassroomCourseWork> findUpcomingCourseWork(@Param("userId") UUID userId, @Param("now") LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT cw FROM ClassroomCourseWork cw WHERE cw.user.id = :userId AND cw.dueDate < :now AND cw.processingState = 'AI_PROCESSED' ORDER BY cw.dueDate ASC")
    List<ClassroomCourseWork> findOverdueCourseWork(@Param("userId") UUID userId, @Param("now") LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT cw FROM ClassroomCourseWork cw WHERE cw.processingState = 'FAILED' AND (cw.nextRetryAt IS NULL OR cw.nextRetryAt <= :now) ORDER BY cw.nextRetryAt ASC")
    List<ClassroomCourseWork> findFailedCourseWorkForRetry(@Param("now") LocalDateTime now, org.springframework.data.domain.Pageable pageable);
}
