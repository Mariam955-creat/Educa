package com.educa.backend.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(1) Integer position) {
}
