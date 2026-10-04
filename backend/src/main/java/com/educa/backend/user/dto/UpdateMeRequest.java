package com.educa.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mise à jour partielle du profil : un champ absent ({@code null}) reste inchangé, une chaîne vide efface
 * les champs facultatifs (titre, biographie, pays, téléphone).
 */
public record UpdateMeRequest(
        @Size(max = 150) String fullName,
        @Pattern(regexp = "fr|en|de|nl", message = "langue non supportée") String preferredLanguage,
        @Size(max = 120) String headline,
        @Size(max = 1000) String bio,
        @Pattern(regexp = "|[A-Z]{2}", message = "code pays invalide") String country,
        @Pattern(regexp = "|[+0-9 ().-]{6,30}", message = "numéro de téléphone invalide") String phone) {
}
