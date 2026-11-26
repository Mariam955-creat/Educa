package com.educa.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Refus d'une demande de rôle formateur : le motif est obligatoire et transmis au candidat. */
public record RejectInstructorRequest(@NotBlank @Size(max = 1000) String reason) {
}
