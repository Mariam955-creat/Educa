package com.educa.backend.quiz;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.enrollment.EnrollmentService;

/**
 * Règle de déverrouillage de l'examen final : 100 % de progression
 * (tous les contenus vus) ET tous les contrôles de chapitre tentés au moins une fois.
 * Service dédié pour éviter un cycle {@code QuizService} ⇄ {@code QuizAttemptService}.
 */
@Service
public class QuizUnlockService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final EnrollmentService enrollmentService;

    public QuizUnlockService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
                             EnrollmentService enrollmentService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.enrollmentService = enrollmentService;
    }

    @Transactional(readOnly = true)
    public boolean isFinalExamUnlocked(Long userId, Long courseId) {
        return enrollmentService.contentsFullyCompleted(userId, courseId)
                && allControlsAttempted(userId, courseId);
    }

    @Transactional(readOnly = true)
    public boolean allControlsAttempted(Long userId, Long courseId) {
        return quizRepository.findByCourseIdAndTypeOrderByIdAsc(courseId, QuizType.CONTROL).stream()
                .allMatch(quiz -> attemptRepository.existsByUserIdAndQuizId(userId, quiz.getId()));
    }
}
