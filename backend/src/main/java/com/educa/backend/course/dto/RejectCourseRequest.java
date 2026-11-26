package com.educa.backend.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Refus d'un cours soumis : le motif est obligatoire et transmis au formateur. */
public record RejectCourseRequest(@NotBlank @Size(max = 2000) String reason) {
}
