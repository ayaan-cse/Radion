package com.radion.repository;

import com.radion.domain.models.ClassroomCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomCourseRepository extends JpaRepository<ClassroomCourse, UUID> {
    Optional<ClassroomCourse> findByGoogleCourseIdAndUserId(String googleCourseId, UUID userId);
}
