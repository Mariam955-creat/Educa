package com.educa.backend.course.dto;

import java.math.BigDecimal;
import java.util.List;

import com.educa.backend.course.CourseCategory;
import com.educa.backend.course.CourseLevel;

public record CourseDetailDto(
        Long id,
        String slug,
        String title,
        String description,
        String language,
        boolean published,
        String instructorName,
        int controlWeight,
        int examWeight,
        int passThreshold,
        boolean contentsVisible,
        List<ChapterDto> chapters,
        BigDecimal price,
        String coverImageUrl,
        long learnerCount,
        Double averageRating,
        long ratingCount,
        String subtitle,
        CourseCategory category,
        CourseLevel level,
        BigDecimal durationHours,
        List<String> objectives,
        List<String> prerequisites,
        String targetAudience,
        String instructorHeadline,
        String instructorBio) {
}
