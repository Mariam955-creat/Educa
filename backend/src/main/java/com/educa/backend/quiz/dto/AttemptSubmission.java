package com.educa.backend.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AttemptSubmission(
        @NotNull @Valid List<AnswerSubmission> answers) {

    public record AnswerSubmission(
            @NotNull Long questionId,
            List<Long> selectedOptionIds) {
    }
}
