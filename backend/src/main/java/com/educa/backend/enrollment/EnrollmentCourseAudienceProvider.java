package com.educa.backend.enrollment;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.educa.backend.course.CourseAudienceProvider;

/**
 * Nombre d'inscrits par cours. Un cours payant n'est accessible qu'après achat : c'est donc aussi
 * son nombre d'acheteurs.
 */
@Component
class EnrollmentCourseAudienceProvider implements CourseAudienceProvider {

    private final EnrollmentRepository enrollmentRepository;

    EnrollmentCourseAudienceProvider(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public Map<Long, Long> learnerCounts(Collection<Long> courseIds) {
        return enrollmentRepository.countByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
