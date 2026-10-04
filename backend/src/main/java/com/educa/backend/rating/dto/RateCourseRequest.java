package com.educa.backend.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param comment avis écrit facultatif ; vide = note seule */
public record RateCourseRequest(@NotNull @Min(1) @Max(5) Integer stars, @Size(max = 2000) String comment) {
}
