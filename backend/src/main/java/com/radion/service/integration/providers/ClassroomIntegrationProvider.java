package com.radion.service.integration.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Announcement;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.radion.domain.enums.MessageProcessingState;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ClassroomAnnouncement;
import com.radion.domain.models.ClassroomCourse;
import com.radion.domain.models.ClassroomCourseWork;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.repository.ClassroomAnnouncementRepository;
import com.radion.repository.ClassroomCourseRepository;
import com.radion.repository.ClassroomCourseWorkRepository;
import com.radion.service.integration.IntegrationProvider;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import com.radion.service.pipeline.reasoning.ClassroomPipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomIntegrationProvider implements IntegrationProvider {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final ClassroomCourseRepository courseRepository;
    private final ClassroomCourseWorkRepository courseWorkRepository;
    private final ClassroomAnnouncementRepository announcementRepository;
    private final ClassroomPipelineOrchestrator classroomPipelineOrchestrator;
    private final ObjectMapper objectMapper;

    @Override
    public Platform getPlatform() {
        return Platform.CLASSROOM;
    }

    @Override
    public boolean refreshTokenIfNeeded(ConnectedService connection) {
        return googleOAuthService.refreshAccessToken(connection);
    }

    @Override
    public int sync(User user, ConnectedService connection) {
        log.info("Starting native Google Classroom sync for user: {}", user.getId());

        if (!refreshTokenIfNeeded(connection)) {
            log.warn("Skipping Classroom sync due to invalid token for user: {}", user.getId());
            return 0;
        }

        if (connection.getLastSyncAt() == null) {
            connection.setLastSyncAt(LocalDateTime.now());
            log.info("lastClassroomSyncAt was null for user {}. Setting to now. Aborting historical sync to respect contract.", user.getId());
            return 0;
        }

        try {
            Classroom classroomService = buildClassroomClient(connection.getAccessToken());
            
            List<Course> courses = classroomService.courses().list()
                    .setCourseStates(List.of("ACTIVE"))
                    .execute()
                    .getCourses();

            if (courses == null || courses.isEmpty()) {
                log.info("No active courses found for user: {}", user.getId());
                return 0;
            }

            int processedCount = 0;

            for (Course apiCourse : courses) {
                // Upsert Course
                ClassroomCourse course = courseRepository.findByGoogleCourseIdAndUserId(apiCourse.getId(), user.getId())
                        .orElse(new ClassroomCourse());
                course.setUser(user);
                course.setGoogleCourseId(apiCourse.getId());
                course.setName(apiCourse.getName());
                course.setStatus(apiCourse.getCourseState());
                if (apiCourse.getUpdateTime() != null) {
                    course.setUpdateTime(LocalDateTime.ofInstant(Instant.parse(apiCourse.getUpdateTime()), ZoneId.systemDefault()));
                }
                courseRepository.save(course);

                processedCount += syncCourseWork(classroomService, course, connection, user);
                processedCount += syncAnnouncements(classroomService, course, connection, user);
            }

            log.info("Successfully synced {} Classroom items for user: {}", processedCount, user.getId());
            return processedCount;

        } catch (Exception e) {
            log.error("Google Classroom API sync failed for user: {}", user.getId(), e);
            throw new RuntimeException("Classroom sync failed", e);
        }
    }

    private int syncCourseWork(Classroom service, ClassroomCourse course, ConnectedService connection, User user) throws Exception {
        List<CourseWork> courseWorkList = service.courses().courseWork().list(course.getGoogleCourseId()).execute().getCourseWork();
        if (courseWorkList == null) return 0;

        int count = 0;
        for (CourseWork work : courseWorkList) {
            if (isNewItem(work.getUpdateTime(), connection)) {
                ClassroomCourseWork dbWork = courseWorkRepository.findByGoogleCourseWorkIdAndUserId(work.getId(), user.getId())
                        .orElse(new ClassroomCourseWork());

                dbWork.setUser(user);
                dbWork.setCourse(course);
                dbWork.setGoogleCourseWorkId(work.getId());
                dbWork.setTitle(work.getTitle());
                dbWork.setDescription(work.getDescription());
                
                if (work.getDueDate() != null && work.getDueTime() != null) {
                    try {
                        LocalDateTime dueDate = LocalDateTime.of(
                            work.getDueDate().getYear(),
                            work.getDueDate().getMonth(),
                            work.getDueDate().getDay(),
                            work.getDueTime().getHours() != null ? work.getDueTime().getHours() : 23,
                            work.getDueTime().getMinutes() != null ? work.getDueTime().getMinutes() : 59
                        );
                        dbWork.setDueDate(dueDate);
                    } catch (Exception e) {
                        log.warn("Failed to parse due date for coursework {}", work.getId());
                    }
                }

                if (work.getUpdateTime() != null) {
                    dbWork.setUpdateTime(LocalDateTime.ofInstant(Instant.parse(work.getUpdateTime()), ZoneId.systemDefault()));
                }
                
                dbWork.setRawPayload(objectMapper.writeValueAsString(work));
                
                if (dbWork.getProcessingState() == null || dbWork.getProcessingState() == MessageProcessingState.NEW) {
                    dbWork.setProcessingState(MessageProcessingState.NEW);
                    // AI Evaluation happens IN MEMORY before DB insert
                    try {
                        classroomPipelineOrchestrator.processCourseWork(dbWork);
                    } catch (Exception e) {
                        log.error("Failed to process coursework immediately", e);
                    }
                } else {
                    courseWorkRepository.save(dbWork);
                }
                count++;
            }
        }
        return count;
    }

    private int syncAnnouncements(Classroom service, ClassroomCourse course, ConnectedService connection, User user) throws Exception {
        List<Announcement> announcements = service.courses().announcements().list(course.getGoogleCourseId()).execute().getAnnouncements();
        if (announcements == null) return 0;

        int count = 0;
        for (Announcement announcement : announcements) {
            if (isNewItem(announcement.getUpdateTime(), connection)) {
                ClassroomAnnouncement dbAnnouncement = announcementRepository.findByGoogleAnnouncementIdAndUserId(announcement.getId(), user.getId())
                        .orElse(new ClassroomAnnouncement());

                dbAnnouncement.setUser(user);
                dbAnnouncement.setCourse(course);
                dbAnnouncement.setGoogleAnnouncementId(announcement.getId());
                dbAnnouncement.setText(announcement.getText());

                if (announcement.getUpdateTime() != null) {
                    dbAnnouncement.setUpdateTime(LocalDateTime.ofInstant(Instant.parse(announcement.getUpdateTime()), ZoneId.systemDefault()));
                }

                dbAnnouncement.setRawPayload(objectMapper.writeValueAsString(announcement));

                if (dbAnnouncement.getProcessingState() == null || dbAnnouncement.getProcessingState() == MessageProcessingState.NEW) {
                    dbAnnouncement.setProcessingState(MessageProcessingState.NEW);
                    // AI Evaluation happens IN MEMORY before DB insert
                    try {
                        classroomPipelineOrchestrator.processAnnouncement(dbAnnouncement);
                    } catch (Exception e) {
                        log.error("Failed to process Announcement immediately", e);
                    }
                } else {
                    announcementRepository.save(dbAnnouncement);
                }

                count++;
            }
        }
        return count;
    }

    private boolean isNewItem(String updateTimeStr, ConnectedService connection) {
        if (connection.getLastSyncAt() == null || updateTimeStr == null) return true;
        Instant updateTime = Instant.parse(updateTimeStr);
        Instant lastSync = connection.getLastSyncAt().atZone(ZoneId.systemDefault()).toInstant();
        return updateTime.isAfter(lastSync);
    }

    private Classroom buildClassroomClient(String accessToken) {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Classroom.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Radion Dashboard")
                .build();
    }
}