package com.educa.backend.course.dto;

import com.educa.backend.course.ContentType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentRequest(
        @NotNull ContentType type,
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(1) Integer position,
        @Size(max = 100_000) String textBody) {
}
