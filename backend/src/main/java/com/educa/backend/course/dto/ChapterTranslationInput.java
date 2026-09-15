package com.educa.backend.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChapterTranslationInput(
        @NotNull Long chapterId,
        @Size(max = 200) String title) {
}
