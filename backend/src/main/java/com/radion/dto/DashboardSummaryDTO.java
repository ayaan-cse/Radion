package com.radion.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSummaryDTO {

    private UserDTO user;
    private String lastSyncTime;
    private int unreadNotifications;

    private List<ConnectionDTO> connections;
    private List<EventDTO> todaysEvents;
    private List<UpcomingEventDTO> upcomingEvents;
    private List<MessageDTO> recentMessages;

    // New Analytics Section
    private AnalyticsDTO analytics;
    
    // Classroom Section
    private List<ClassroomAssignmentDTO> upcomingAssignments;
    private List<ClassroomAssignmentDTO> overdueAssignments;
    private List<ClassroomAnnouncementDTO> recentAnnouncements;

    @Data
    @Builder
    public static class UserDTO {
        private String firstName;
        private String avatarUrl;
    }

    @Data
    @Builder
    public static class AnalyticsDTO {
        private int totalEventsAutomated;
        private int tasksPending;
        private double averageAiConfidence;
        private int hoursSaved;
    }

    @Data
    @Builder
    public static class ClassroomAssignmentDTO {
        private String id;
        private String courseName;
        private String title;
        private String dueDate;
        private String status; // e.g. "OVERDUE", "UPCOMING"
    }

    @Data
    @Builder
    public static class ClassroomAnnouncementDTO {
        private String id;
        private String courseName;
        private String text;
        private String postedAt;
    }
}