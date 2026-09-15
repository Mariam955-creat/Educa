package com.educa.backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.course.dto.CourseTranslationEditDto;
import com.educa.backend.course.dto.UpdateCourseTranslationRequest;

import jakarta.validation.Valid;

/** Gestion des traductions d'un cours (propriétaire / ADMIN uniquement). */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/translations")
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public class CourseTranslationController {

    private final CourseTranslationService translationService;

    public CourseTranslationController(CourseTranslationService translationService) {
        this.translationService = translationService;
    }

    @GetMapping
    public List<String> languages(@PathVariable Long courseId) {
        return translationService.listLanguages(courseId);
    }

    @GetMapping("/{lang}")
    public CourseTranslationEditDto edit(@PathVariable Long courseId, @PathVariable String lang) {
        return translationService.edit(courseId, lang);
    }

    @PutMapping("/{lang}")
    public CourseTranslationEditDto save(@PathVariable Long courseId, @PathVariable String lang,
                                         @Valid @RequestBody UpdateCourseTranslationRequest request) {
        return translationService.save(courseId, lang, request);
    }

    @DeleteMapping("/{lang}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long courseId, @PathVariable String lang) {
        translationService.delete(courseId, lang);
    }
}
