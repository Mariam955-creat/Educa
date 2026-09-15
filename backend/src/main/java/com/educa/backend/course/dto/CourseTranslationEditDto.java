package com.educa.backend.course.dto;

import java.util.List;

/** Vue d'édition d'une langue de traduction : titre/description du cours + titre de chaque chapitre. */
public record CourseTranslationEditDto(
        String languageCode,
        String courseTitle,
        String courseDescription,
        List<ChapterTranslationItem> chapters) {
}
