package com.radion.domain.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "classroom_courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false)
    private String googleCourseId;

    private String name;

    private String status;

    private LocalDateTime updateTime;
}
