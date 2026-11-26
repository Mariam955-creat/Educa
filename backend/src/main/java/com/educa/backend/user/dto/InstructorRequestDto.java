package com.educa.backend.user.dto;

import java.time.Instant;

/** Demande de rôle formateur, vue par l'administration ou par son auteur. */
public record InstructorRequestDto(
        Long id,
        Long userId,
        String fullName,
        String email,
        String headline,
        String motivation,
        String status,
        String rejectionReason,
        Instant createdAt,
        Instant decidedAt) {
}
