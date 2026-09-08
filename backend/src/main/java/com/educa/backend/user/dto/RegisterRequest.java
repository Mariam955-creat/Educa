package com.educa.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "fr|en|ar", message = "langue non supportée") String preferredLanguage) {
}
