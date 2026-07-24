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
    Optional<Event> findByUserIdAndCompanyOrSourceAndEventTime(UUID userId, String company, LocalDateTime time);
    List<Event> findByUserIdAndCompanyOrSourceOrderByEventTimeAsc(UUID userId, String company);
}