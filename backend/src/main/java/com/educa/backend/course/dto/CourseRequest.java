package com.educa.backend.course.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création (POST) et remplacement (PUT) d'un cours. {@code price} reste optionnel côté API (défaut
 * gratuit si absent) — le formulaire formateur, lui, l'exige toujours (0 = gratuit, explicite).
 */
public record CourseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10_000) String description,
        @Pattern(regexp = "fr|en|ar") String language,
        @Min(0) @Max(100) Integer controlWeight,
        @Min(0) @Max(100) Integer examWeight,
        @Min(0) @Max(100) Integer passThreshold,
        @DecimalMin(value = "0", message = "Le prix ne peut pas être négatif") BigDecimal price) {
}
