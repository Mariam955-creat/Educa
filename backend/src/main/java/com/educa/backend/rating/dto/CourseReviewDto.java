package com.educa.backend.rating.dto;

import java.time.Instant;

/** Avis publié sur la page d'un cours. */
public record CourseReviewDto(Long id, String authorName, int stars, String comment, Instant updatedAt) {
}
