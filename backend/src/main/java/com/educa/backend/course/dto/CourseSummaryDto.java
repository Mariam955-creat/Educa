package com.educa.backend.course.dto;

public record CourseSummaryDto(
        Long id,
        String slug,
        String title,
        String description,
        String language,
        boolean published,
        String instructorName,
        int chapterCount) {
}
