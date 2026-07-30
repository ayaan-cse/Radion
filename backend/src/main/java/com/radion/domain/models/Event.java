package com.radion.domain.models;

import com.radion.domain.enums.EventCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_message_id")
    private Message sourceMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_course_work_id")
    private ClassroomCourseWork sourceCourseWork;

    private String title;

    private String companyOrSource;

    @Enumerated(EnumType.STRING)
    private EventCategory category;

    private LocalDateTime eventTime;

    // Google Calendar event ID for sync/update/delete
    @Column(length = 1024)
    private String googleCalendarEventId;

    // Groups related events together
    // Example: TCS-Placement-2026
    private String timelineGroupId;

    // Prevent AI from overwriting manual user edits
    private boolean isUserModified;

    // Track Google Calendar sync status
    @Column(length = 50)
    private String calendarSyncStatus;

    @Column(length = 2048)
    private String calendarSyncError;

    @Builder.Default
    @Column(columnDefinition = "integer default 0")
    private Integer retryCount = 0;

    private LocalDateTime nextRetryAt;
}