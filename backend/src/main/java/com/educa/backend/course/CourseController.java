package com.educa.backend.course;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.web.PageResponse;
import com.educa.backend.course.dto.CourseDetailDto;
import com.educa.backend.course.dto.CourseRequest;
import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public CourseController(CourseService courseService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    // ---------- catalogue public ----------

    @GetMapping("/courses")
    public PageResponse<CourseSummaryDto> catalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(courseService.catalog(q, language, PageRequest.of(Math.max(page, 0), safeSize)));
    }

    @GetMapping("/courses/{slug}")
    public CourseDetailDto detail(@PathVariable String slug) {
        Long userId = CurrentUser.optionalId();
        Long courseId = courseService.publicIdBySlug(slug);
        boolean enrolled = userId != null && enrollmentService.isEnrolled(userId, courseId);
        return courseService.getDetailBySlug(slug, enrolled);
    }

    // ---------- espace formateur ----------

    @GetMapping("/instructor/courses")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public List<CourseSummaryDto> myCourses() {
        return courseService.listByInstructor(CurrentUser.id());
    }

    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseSummaryDto create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(CurrentUser.id(), request);
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }

    @PostMapping("/courses/{id}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto publish(@PathVariable Long id) {
        return courseService.setPublished(id, true);
    }

    @PostMapping("/courses/{id}/unpublish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto unpublish(@PathVariable Long id) {
        return courseService.setPublished(id, false);
    }
}
