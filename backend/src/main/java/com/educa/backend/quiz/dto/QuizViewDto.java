package com.educa.backend.quiz.dto;

import java.util.List;

public record QuizViewDto(
        Long id,
        String type,
        String title,
        int passThreshold,
        Integer maxAttempts,
        boolean answersVisible,
        List<QuestionViewDto> questions) {

    public record QuestionViewDto(
            Long id,
            String statement,
            String type,
            int points,
            int position,
            List<OptionViewDto> options) {
    }

    public record OptionViewDto(
            Long id,
            String label,
            int position,
            Boolean correct) {
    }
}
