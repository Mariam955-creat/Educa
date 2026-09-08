package com.educa.backend.quiz;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByChapterIdAndType(Long chapterId, QuizType type);

    Optional<Quiz> findByCourseIdAndType(Long courseId, QuizType type);

    List<Quiz> findByCourseIdAndTypeOrderByIdAsc(Long courseId, QuizType type);

    boolean existsByChapterIdAndType(Long chapterId, QuizType type);

    boolean existsByCourseIdAndType(Long courseId, QuizType type);
}
