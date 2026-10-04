package com.educa.backend.course;

import org.springframework.stereotype.Component;

import com.educa.backend.user.UserDeletionGuard;

/** Un formateur qui possède des cours (même à la corbeille) ne peut pas être supprimé définitivement. */
@Component
class InstructorDeletionGuard implements UserDeletionGuard {

    private final CourseRepository courseRepository;

    InstructorDeletionGuard(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public String blockingReason(Long userId) {
        return courseRepository.countByInstructorId(userId) > 0
                ? "Ce compte est formateur de cours : supprimez ou réattribuez d'abord ses cours"
                : null;
    }
}
