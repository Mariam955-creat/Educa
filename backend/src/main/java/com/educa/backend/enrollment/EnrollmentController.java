package com.educa.backend.enrollment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.enrollment.dto.CourseProgressDto;
import com.educa.backend.enrollment.dto.EnrollmentDto;
import com.educa.backend.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/courses/{courseId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentDto enroll(@PathVariable Long courseId) {
        return enrollmentService.enroll(CurrentUser.id(), courseId);
    }

    @GetMapping("/enrollments/me")
    public List<EnrollmentDto> myEnrollments() {
        return enrollmentService.myEnrollments(CurrentUser.id());
    }

    @PostMapping("/contents/{contentId}/complete")
    public CourseProgressDto complete(@PathVariable Long contentId) {
        return enrollmentService.completeContent(CurrentUser.id(), contentId);
    }

    @GetMapping("/courses/{courseId}/progress")
    public CourseProgressDto progress(@PathVariable Long courseId) {
        return enrollmentService.courseProgress(CurrentUser.id(), courseId);
    }
}
