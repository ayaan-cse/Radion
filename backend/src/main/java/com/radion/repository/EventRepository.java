package com.radion.repository;
import com.radion.domain.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByUserIdAndEventTimeBetweenOrderByEventTimeAsc(UUID userId, LocalDateTime start, LocalDateTime end);
    List<Event> findByUserIdAndEventTimeAfterOrderByEventTimeAsc(UUID userId, LocalDateTime time);
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.companyOrSource = :company AND e.eventTime = :time")
    Optional<Event> findByUserIdAndCompanyOrSourceAndEventTime(UUID userId, String company, LocalDateTime time);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.companyOrSource = :company ORDER BY e.eventTime ASC")
    List<Event> findByUserIdAndCompanyOrSourceOrderByEventTimeAsc(UUID userId, String company);
    
    Optional<Event> findBySourceCourseWorkId(UUID courseWorkId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Event e WHERE e.calendarSyncStatus = 'FAILED' AND e.retryCount < :maxRetries AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)")
    List<Event> findFailedEventsForRetry(int maxRetries, LocalDateTime now);
}