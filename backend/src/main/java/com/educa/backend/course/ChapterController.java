package com.educa.backend.course;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.course.dto.ChapterDto;
import com.educa.backend.course.dto.ChapterRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/courses/{courseId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterDto create(@PathVariable Long courseId, @Valid @RequestBody ChapterRequest request) {
        return chapterService.create(courseId, request);
    }

    @PutMapping("/chapters/{id}")
    public ChapterDto update(@PathVariable Long id, @Valid @RequestBody ChapterRequest request) {
        return chapterService.update(id, request);
    }

    @DeleteMapping("/chapters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        chapterService.delete(id);
    }
}
