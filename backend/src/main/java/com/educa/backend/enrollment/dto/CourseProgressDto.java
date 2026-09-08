package com.educa.backend.enrollment.dto;

import java.util.List;

public record CourseProgressDto(
        Long courseId,
        int totalContents,
        int completedContents,
        int progressPercent,
        List<Long> completedContentIds) {
}
