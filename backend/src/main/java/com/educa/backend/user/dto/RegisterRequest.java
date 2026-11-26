package com.educa.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String password,
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "fr|en|de|nl", message = "langue non supportée") String preferredLanguage,
        @Pattern(regexp = "[A-Z]{2}", message = "code pays invalide") String country) {
}
