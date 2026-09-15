package com.educa.backend.course.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCourseTranslationRequest(
        @NotBlank @Size(max = 200) String courseTitle,
        @Size(max = 10_000) String courseDescription,
        List<@Valid ChapterTranslationInput> chapters) {
}
