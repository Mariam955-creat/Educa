package com.educa.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    boolean existsByQuizIdAndPosition(Long quizId, int position);
}
