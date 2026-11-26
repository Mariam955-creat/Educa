package com.educa.backend.user.dto;

import jakarta.validation.constraints.Size;

/** Demande de rôle formateur faite depuis l'espace compte ; la motivation est facultative. */
public record CreateInstructorRequest(@Size(max = 2000) String motivation) {
}
