package com.educa.backend.course;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.web.PageResponse;
import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.course.dto.RejectCourseRequest;

import jakarta.validation.Valid;

/** Validation des cours : soumission par le formateur, file d'attente et décision par l'administration. */
@RestController
@RequestMapping("/api/v1")
public class CourseReviewController {

    private final CourseReviewService service;

    public CourseReviewController(CourseReviewService service) {
        this.service = service;
    }

    /** Le formateur soumet son cours (ou le resoumet après un refus). */
    @PostMapping("/courses/{id}/submit-review")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto submit(@PathVariable Long id) {
        return service.submit(id);
    }

    /** Cours en attente de validation, les plus anciennement soumis d'abord. */
    @GetMapping("/admin/course-reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CourseSummaryDto> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(service.pending(
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.ASC, "submittedAt"))));
    }

    /** Nombre de cours en attente (pastille du menu admin). */
    @GetMapping("/admin/course-reviews/count")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> count() {
        return Map.of("pending", service.countPending());
    }

    @PostMapping("/admin/course-reviews/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseSummaryDto approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/admin/course-reviews/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseSummaryDto reject(@PathVariable Long id, @Valid @RequestBody RejectCourseRequest request) {
        return service.reject(id, request.reason());
    }
}
