package com.educa.backend.quiz.dto;

import java.util.List;

public record CourseQuizzesDto(
        List<QuizRefDto> controls,
        QuizRefDto finalExam) {

    public record QuizRefDto(
            Long quizId,
            Long chapterId,
            String type,
            String title,
            int questionCount,
            Integer maxAttempts) {
    }
}
