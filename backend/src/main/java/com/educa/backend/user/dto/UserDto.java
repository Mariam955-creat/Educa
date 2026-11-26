package com.educa.backend.user.dto;

import java.time.Instant;
import java.util.Set;

/** Profil de l'utilisateur connecté (lui seul le reçoit : il contient son téléphone). */
public record UserDto(
        Long id,
        String email,
        String fullName,
        String preferredLanguage,
        Set<String> roles,
        String headline,
        String bio,
        String country,
        String phone,
        /** Demande de rôle formateur en attente ; {@code null} si aucune. */
        Instant instructorRequestedAt) {
}
