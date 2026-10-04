package com.educa.backend.user.dto;

/** Profil public d'un utilisateur (formateur d'un cours) : jamais d'email ni de téléphone. */
public record PublicProfileDto(String fullName, String headline, String bio) {
}
