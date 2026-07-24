package com.radion.service.engine;

import com.radion.domain.models.Task;
import com.radion.domain.models.User;
import com.radion.repository.TaskRepository;
import com.radion.service.pipeline.models.AIExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEngine {

    private final TaskRepository taskRepository;

    @Transactional
    public Task createTask(User user, AIExtractionResult extraction, String source) {
        log.info("Creating task for user {}: {}", user.getId(), extraction.getAssignmentName());
        
        LocalDateTime dueDate = null;
        if (extraction.getEventDate() != null) {
            dueDate = extraction.getEventTime() != null 
                ? extraction.getEventDate().atTime(extraction.getEventTime()) 
                : extraction.getEventDate().atTime(23, 59); // Default to EOD
        }

        Task task = Task.builder()
                .user(user)
                .title(extraction.getAssignmentName() != null ? extraction.getAssignmentName() : extraction.getSubject())
                .source(source)
                .dueDate(dueDate)
                .isCompleted(false)
                .build();

        return taskRepository.save(task);
    }
}