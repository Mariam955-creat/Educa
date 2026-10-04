package com.educa.backend.course;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.web.PageResponse;
import com.educa.backend.course.dto.CourseSummaryDto;

/** Tous les cours de la plateforme, publiés ou non (administration). */
@RestController
@RequestMapping("/api/v1/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** @param published filtre facultatif : {@code true} publiés, {@code false} brouillons, absent = tous */
    @GetMapping
    public PageResponse<CourseSummaryDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(courseService.adminList(q, published, PageRequest.of(Math.max(page, 0), safeSize)));
    }
}
