package com.educa.backend.course.dto;

import java.util.List;

public record CourseDetailDto(
        Long id,
        String slug,
        String title,
        String description,
        String language,
        boolean published,
        String instructorName,
        int controlWeight,
        int examWeight,
        int passThreshold,
        boolean contentsVisible,
        List<ChapterDto> chapters) {
}
