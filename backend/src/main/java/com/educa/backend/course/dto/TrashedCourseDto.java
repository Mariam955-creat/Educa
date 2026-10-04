package com.educa.backend.course.dto;

import java.time.Instant;

/** Cours à la corbeille. {@code learnerCount > 0} : suppression définitive impossible (inscrits). */
public record TrashedCourseDto(Long id, String title, String instructorName, String coverImageUrl, long learnerCount,
                               Instant deletedAt) {
}
