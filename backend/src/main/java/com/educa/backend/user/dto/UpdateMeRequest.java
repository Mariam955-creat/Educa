package com.educa.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(max = 150) String fullName,
        @Pattern(regexp = "fr|en|ar", message = "langue non supportée") String preferredLanguage) {
}
