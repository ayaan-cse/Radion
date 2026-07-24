package com.radion.service.integration.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Announcement;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.radion.domain.enums.Platform;
import com.radion.domain.models.ConnectedService;
import com.radion.domain.models.User;
import com.radion.service.integration.IntegrationProvider;
import com.radion.service.integration.oauth.GoogleOAuthServiceImpl;
import com.radion.service.pipeline.InformationCollectionEngine;
import com.radion.service.pipeline.models.RawPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomIntegrationProvider implements IntegrationProvider {

    private final GoogleOAuthServiceImpl googleOAuthService;
    private final InformationCollectionEngine pipelineEngine;
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
    public void sync(User user, ConnectedService connection) {
        log.info("Starting real Google Classroom sync for user: {}", user.getId());

        if (!refreshTokenIfNeeded(connection)) {
            log.warn("Skipping Classroom sync due to invalid token for user: {}", user.getId());
            return;
        }

        try {
            Classroom classroomService = buildClassroomClient(connection.getAccessToken());
            
            // 1. Fetch Active Courses
            List<Course> courses = classroomService.courses().list()
                    .setCourseStates(List.of("ACTIVE"))
                    .execute()
                    .getCourses();

            if (courses == null || courses.isEmpty()) {
                log.info("No active courses found for user: {}", user.getId());
                return;
            }

            int processedCount = 0;

            // 2. Iterate through courses to fetch CourseWork and Announcements
            for (Course course : courses) {
                processedCount += syncCourseWork(classroomService, course, connection);
                processedCount += syncAnnouncements(classroomService, course, connection);
            }

            log.info("Successfully synced {} Classroom items for user: {}", processedCount, user.getId());

        } catch (Exception e) {
            log.error("Google Classroom API sync failed for user: {}", user.getId(), e);
            throw new RuntimeException("Classroom sync failed", e);
        }
    }

    private int syncCourseWork(Classroom service, Course course, ConnectedService connection) throws Exception {
        List<CourseWork> courseWorkList = service.courses().courseWork().list(course.getId()).execute().getCourseWork();
        if (courseWorkList == null) return 0;

        int count = 0;
        for (CourseWork work : courseWorkList) {
            if (isNewItem(work.getUpdateTime(), connection)) {
                sendToPipeline("COURSE_WORK", course.getName(), work.getId(), work, work.getUpdateTime());
                count++;
            }
        }
        return count;
    }

    private int syncAnnouncements(Classroom service, Course course, ConnectedService connection) throws Exception {
        List<Announcement> announcements = service.courses().announcements().list(course.getId()).execute().getAnnouncements();
        if (announcements == null) return 0;

        int count = 0;
        for (Announcement announcement : announcements) {
            if (isNewItem(announcement.getUpdateTime(), connection)) {
                sendToPipeline("ANNOUNCEMENT", course.getName(), announcement.getId(), announcement, announcement.getUpdateTime());
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

    private void sendToPipeline(String type, String courseName, String externalId, Object item, String updateTime) {
        try {
            // Wrap the Google API object with context (courseName, type) for the parser
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("type", type);
            rootNode.put("courseName", courseName);
            rootNode.put("updateTime", updateTime);
            rootNode.set("item", objectMapper.valueToTree(item));

            RawPayload payload = RawPayload.builder()
                    .externalMessageId(externalId)
                    .platform(Platform.CLASSROOM)
                    .rawJsonContent(objectMapper.writeValueAsString(rootNode))
                    .build();

            pipelineEngine.processRawPayload(payload);
        } catch (Exception e) {
            log.error("Failed to serialize Classroom item for pipeline", e);
        }
    }

    private Classroom buildClassroomClient(String accessToken) {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        return new Classroom.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Radion Dashboard")
                .build();
    }
}