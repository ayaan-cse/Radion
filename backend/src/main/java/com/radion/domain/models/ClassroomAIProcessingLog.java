package com.radion.domain.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "classroom_ai_processing_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomAIProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_work_id", unique = true, nullable = true)
    private ClassroomCourseWork courseWork;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", unique = true, nullable = true)
    private ClassroomAnnouncement announcement;

    @Column(columnDefinition = "TEXT")
    private String extractedJson;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    private Double confidenceScore;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
