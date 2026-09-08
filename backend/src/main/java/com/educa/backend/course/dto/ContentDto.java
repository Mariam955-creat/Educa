package com.educa.backend.course.dto;

import com.educa.backend.course.ContentType;

public record ContentDto(
        Long id,
        ContentType type,
        String title,
        int position,
        String textBody,
        String fileName,
        String mimeType,
        boolean hasFile) {
}
