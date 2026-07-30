package com.radion.domain.models;

import com.radion.domain.enums.MessageProcessingState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "classroom_course_work")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomCourseWork {

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
    private String googleCourseWorkId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime dueDate;

    private LocalDateTime updateTime;

    @Enumerated(EnumType.STRING)
    private MessageProcessingState processingState;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Builder.Default
    private int retryCount = 0;

    private LocalDateTime nextRetryAt;
}
