package com.educa.backend.course.dto;

import java.math.BigDecimal;

import com.educa.backend.course.CourseCategory;
import com.educa.backend.course.CourseLevel;

/**
 * @param coverImageUrl  URL de l'image de couverture ({@code null} si aucune) — versionnée pour le cache navigateur
 * @param averageRating  note moyenne sur 5, {@code null} tant que personne n'a noté
 */
public record CourseSummaryDto(
        Long id,
        String slug,
        String title,
        String description,
        String language,
        boolean published,
        String instructorName,
        int chapterCount,
        BigDecimal price,
        String coverImageUrl,
        long learnerCount,
        Double averageRating,
        long ratingCount,
        String subtitle,
        CourseCategory category,
        CourseLevel level,
        BigDecimal durationHours) {
}
