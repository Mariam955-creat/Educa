package com.educa.backend.rating.dto;

import java.time.Instant;

/** Avis de l'utilisateur courant, listé dans son espace « Mon compte ». {@code comment} est {@code null} pour une note seule. */
public record MyReviewDto(Long id, Long courseId, String courseTitle, String courseSlug, int stars, String comment,
                          Instant updatedAt) {
}
