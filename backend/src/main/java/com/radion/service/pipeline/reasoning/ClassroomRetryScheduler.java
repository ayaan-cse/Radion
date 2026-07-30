package com.radion.service.pipeline.reasoning;

import com.radion.domain.models.ClassroomCourseWork;
import com.radion.repository.ClassroomCourseWorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomRetryScheduler {

    private final ClassroomCourseWorkRepository courseWorkRepository;
    private final ClassroomPipelineOrchestrator orchestrator;

    /**
     * Runs every minute to retry CourseWork items that failed with a temporary error (e.g. 429, 503).
     * Items are fetched if their nextRetryAt is null or in the past.
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedCourseWork() {
        log.info("ClassroomRetryScheduler is running...");

        try {
            List<ClassroomCourseWork> failedItems = courseWorkRepository.findFailedCourseWorkForRetry(LocalDateTime.now(), PageRequest.of(0, 10));

            if (failedItems.isEmpty()) {
                return;
            }

            log.info("Found {} failed Classroom CourseWork items to retry.", failedItems.size());

            for (ClassroomCourseWork courseWork : failedItems) {
                log.info("Retrying CourseWork ID: {} (Retry Count: {})", courseWork.getId(), courseWork.getRetryCount());
                try {
                    orchestrator.processCourseWork(courseWork);
                } catch (Exception e) {
                    log.error("Failed to retry CourseWork {}: {}", courseWork.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("ClassroomRetryScheduler encountered a critical error", e);
        }
    }
}
