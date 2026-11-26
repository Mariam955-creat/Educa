package com.educa.backend.user.dto;

import java.time.Instant;
import java.util.List;

/** Demande de rôle formateur, vue par l'administration ou par son auteur, avec ses pièces jointes. */
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
        Instant decidedAt,
        List<Document> documents) {

    /** Pièce jointe : {@code kind} = DIPLOMA, CV ou ID_DOCUMENT. */
    public record Document(Long id, String kind, String originalName, String mimeType, long sizeBytes) {
    }
}
