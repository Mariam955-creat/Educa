package com.educa.backend.enrollment;

import org.springframework.stereotype.Component;

import com.educa.backend.course.CourseDeletionGuard;

/**
 * Interdit la suppression d'un cours qui a des inscrits : les clés étrangères sont en
 * {@code ON DELETE CASCADE}, la suppression effacerait leurs progressions, certificats et paiements.
 */
@Component
class EnrollmentCourseDeletionGuard implements CourseDeletionGuard {

    private final EnrollmentRepository enrollmentRepository;

    EnrollmentCourseDeletionGuard(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public boolean blocksDeletion(Long courseId) {
        return enrollmentRepository.existsByCourseId(courseId);
    }
}
