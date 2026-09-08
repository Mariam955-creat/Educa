package com.educa.backend.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizRequest(
        @NotBlank @Size(max = 200) String title,
        @Min(0) @Max(100) Integer passThreshold,
        @Min(1) Integer maxAttempts) {
}
