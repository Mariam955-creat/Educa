package com.educa.backend.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptionRequest(
        @NotBlank String label,
        boolean correct,
        @NotNull @Min(1) Integer position) {
}
