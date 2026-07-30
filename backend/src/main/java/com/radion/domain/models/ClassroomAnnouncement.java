package com.radion.domain.models;

import com.radion.domain.enums.MessageProcessingState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "classroom_announcements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private ClassroomCourse course;

    @Column(unique = true, nullable = false)
    private String googleAnnouncementId;

    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDateTime updateTime;

    /** AI-extracted date if announcement contains exam/viva/seminar/presentation date */
    private LocalDateTime extractedEventDate;

    /** Google Calendar Event ID if an event was created from this announcement */
    private String googleCalendarEventId;

    @Enumerated(EnumType.STRING)
    private MessageProcessingState processingState;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Builder.Default
    private int retryCount = 0;

    private LocalDateTime nextRetryAt;
}

