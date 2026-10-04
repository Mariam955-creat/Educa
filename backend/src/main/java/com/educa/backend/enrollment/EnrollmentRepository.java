package com.educa.backend.enrollment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentStatus status);

    List<Enrollment> findByUserIdOrderByEnrolledAtDesc(Long userId);

    List<Enrollment> findByCourseId(Long courseId);

    boolean existsByCourseId(Long courseId);

    /** Lignes {@code [courseId, nombre d'inscrits]} pour les cours donnés. */
    @Query("select e.courseId, count(e) from Enrollment e where e.courseId in :courseIds group by e.courseId")
    List<Object[]> countByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
