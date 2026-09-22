package com.educa.backend.course.dto;

import java.math.BigDecimal;

public record CourseSummaryDto(
        Long id,
        String slug,
        String title,
        String description,
        String language,
        boolean published,
        String instructorName,
        int chapterCount,
        BigDecimal price) {
}
