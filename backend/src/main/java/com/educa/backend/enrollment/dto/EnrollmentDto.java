package com.educa.backend.enrollment.dto;

import java.time.Instant;

public record EnrollmentDto(
        Long courseId,
        String courseSlug,
        String courseTitle,
        String status,
        int totalContents,
        int completedContents,
        int progressPercent,
        Instant enrolledAt) {
}
