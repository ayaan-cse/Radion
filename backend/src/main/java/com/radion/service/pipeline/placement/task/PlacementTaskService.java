package com.radion.service.pipeline.placement.task;

import com.radion.domain.models.Message;
import com.radion.domain.models.Task;
import com.radion.domain.models.User;
import com.radion.repository.TaskRepository;
import com.radion.service.pipeline.placement.dto.PlacementExtractionDTO;
import com.radion.service.pipeline.placement.timeline.CompanyMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementTaskService {

    private final TaskRepository taskRepository;
    private final CompanyMatcher companyMatcher;

    /**
     * Creates actionable student tasks based on high-confidence placement extractions.
     * Uses structured business keys (company + stage + deadline) to ensure different stages exist separately
     * while reminder emails update existing tasks instead of duplicating.
     *
     * @param user The student user.
     * @param message The source message.
     * @param extraction The Gemini extraction DTO.
     * @return List of created or updated Task entities.
     */
    @Transactional
    public List<Task> createTasksIfActionable(User user, Message message, PlacementExtractionDTO extraction) {
        List<Task> createdTasks = new ArrayList<>();
        String company = extraction.getCompany() != null && !"null".equalsIgnoreCase(extraction.getCompany()) 
                         ? extraction.getCompany() : "Placement";
        String source = message.getPlatform() != null ? message.getPlatform().name() : "EMAIL";

        String normCompany = companyMatcher.normalize(company);
        String stageKey = extraction.getStage() != null && !extraction.getStage().isBlank() && !"null".equalsIgnoreCase(extraction.getStage())
                          ? extraction.getStage().trim().toUpperCase() : "GENERAL";

        // 1. Check Registration / Application Deadline
        if (extraction.getDeadline() != null && !extraction.getDeadline().isBlank() && !"null".equalsIgnoreCase(extraction.getDeadline())) {
            LocalDateTime dueDate = parseDateTime(extraction.getDeadline());
            String title = "Apply for " + company + (extraction.getRole() != null && !"null".equalsIgnoreCase(extraction.getRole()) ? " - " + extraction.getRole() : "");
            String businessKey = normCompany + "_" + stageKey + "_DEADLINE_" + formatDeadlineDate(extraction.getDeadline());
            Task task = buildOrUpdateTask(user, title, source, dueDate, businessKey);
            if (task != null) createdTasks.add(task);
        }

        // 2. Check Assessment Date
        if (extraction.getAssessmentDate() != null && !extraction.getAssessmentDate().isBlank() && !"null".equalsIgnoreCase(extraction.getAssessmentDate())) {
            LocalDateTime dueDate = parseDateTime(extraction.getAssessmentDate());
            String title = "Online Assessment: " + company;
            String businessKey = normCompany + "_" + stageKey + "_ASSESSMENT_" + formatDeadlineDate(extraction.getAssessmentDate());
            Task task = buildOrUpdateTask(user, title, source, dueDate, businessKey);
            if (task != null) createdTasks.add(task);
        }

        // 3. Check Interview Date
        if (extraction.getInterviewDate() != null && !extraction.getInterviewDate().isBlank() && !"null".equalsIgnoreCase(extraction.getInterviewDate())) {
            LocalDateTime dueDate = parseDateTime(extraction.getInterviewDate());
            String title = "Interview Round: " + company;
            String businessKey = normCompany + "_" + stageKey + "_INTERVIEW_" + formatDeadlineDate(extraction.getInterviewDate());
            Task task = buildOrUpdateTask(user, title, source, dueDate, businessKey);
            if (task != null) createdTasks.add(task);
        }

        // 4. General Action Required (if no dates triggered a task above)
        if (createdTasks.isEmpty() && extraction.isActionRequired()) {
            String title = "Action Required: " + company + " (" + (message.getTitle() != null ? message.getTitle() : "Update") + ")";
            String businessKey = normCompany + "_" + stageKey + "_ACTION_NONE";
            Task task = buildOrUpdateTask(user, title, source, LocalDateTime.now().plusDays(2), businessKey);
            if (task != null) createdTasks.add(task);
        }

        return createdTasks;
    }

    private Task buildOrUpdateTask(User user, String title, String source, LocalDateTime dueDate, String businessKey) {
        if (businessKey != null && !businessKey.isBlank()) {
            var existingOpt = taskRepository.findByUserIdAndBusinessKey(user.getId(), businessKey);
            if (existingOpt.isPresent()) {
                Task existing = existingOpt.get();
                log.info("Reminder email detected for task {} (Business Key: {}). Updating existing task instead of duplicating.", 
                         existing.getId(), businessKey);
                if (dueDate != null) {
                    existing.setDueDate(dueDate);
                }
                existing.setTitle(title);
                return taskRepository.save(existing);
            }
        }

        if (taskRepository.existsByUserIdAndTitleIgnoreCase(user.getId(), title)) {
            log.info("Skipping duplicate task creation for user {}: Task '{}' already exists.", user.getId(), title);
            return null;
        }

        Task task = Task.builder()
                .user(user)
                .title(title)
                .source(source)
                .dueDate(dueDate != null ? dueDate : LocalDateTime.now().plusDays(3))
                .isCompleted(false)
                .businessKey(businessKey)
                .build();
        Task saved = taskRepository.save(task);
        log.info("Created task for user {}: {} (Business Key: {})", user.getId(), title, businessKey);
        return saved;
    }

    private String formatDeadlineDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return "none";
        String clean = dateStr.trim();
        int tIdx = clean.indexOf('T');
        if (tIdx != -1) {
            clean = clean.substring(0, tIdx);
        }
        return clean.toLowerCase();
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return LocalDateTime.now().plusDays(3);
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr.trim(), DateTimeFormatter.ISO_DATE_TIME);
            }
            LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_DATE);
            return date.atTime(LocalTime.of(23, 59));
        } catch (DateTimeParseException e) {
            return LocalDateTime.now().plusDays(5);
        }
    }
}

