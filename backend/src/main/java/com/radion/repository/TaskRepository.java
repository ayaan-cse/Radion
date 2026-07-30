package com.radion.repository;
import com.radion.domain.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    boolean existsByUserIdAndTitleIgnoreCase(UUID userId, String title);
    java.util.List<Task> findByUserIdAndTitleIgnoreCase(UUID userId, String title);
    Optional<Task> findByUserIdAndBusinessKey(UUID userId, String businessKey);
    java.util.List<Task> findByUserIdAndIsCompletedFalse(UUID userId);
    Optional<Task> findByBusinessKey(String businessKey);
}