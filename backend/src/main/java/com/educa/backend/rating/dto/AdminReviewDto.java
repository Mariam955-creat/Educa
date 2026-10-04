package com.educa.backend.rating.dto;

import java.time.Instant;

/** Ligne du registre de modération des avis (administration). {@code comment} est {@code null} pour une note seule. */
public record AdminReviewDto(Long id, Long courseId, String courseTitle, String courseSlug, String authorName,
                             int stars, String comment, Instant updatedAt) {
}
