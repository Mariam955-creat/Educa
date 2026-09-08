package com.educa.backend.quiz.dto;

import java.util.List;

import com.educa.backend.quiz.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
        @NotNull @Size(max = 2000) String statement,
        @NotNull QuestionType type,
        @Min(1) Integer points,
        @NotNull @Min(1) Integer position,
        @NotEmpty @Valid List<OptionRequest> options) {
}
