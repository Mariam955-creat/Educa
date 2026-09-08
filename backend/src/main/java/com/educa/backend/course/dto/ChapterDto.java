package com.educa.backend.course.dto;

import java.util.List;

public record ChapterDto(
        Long id,
        String title,
        int position,
        List<ContentDto> contents) {
}
