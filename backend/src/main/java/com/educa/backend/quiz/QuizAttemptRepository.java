package com.educa.backend.quiz;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdAndQuizIdOrderBySubmittedAtDesc(Long userId, Long quizId);

    long countByUserIdAndQuizId(Long userId, Long quizId);

    boolean existsByUserIdAndQuizId(Long userId, Long quizId);
}
