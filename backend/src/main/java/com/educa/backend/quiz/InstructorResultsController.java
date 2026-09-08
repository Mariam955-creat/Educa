package com.educa.backend.quiz;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.course.CourseService;
import com.educa.backend.quiz.dto.LearnerResultDto;

@RestController
@RequestMapping("/api/v1/instructor")
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public class InstructorResultsController {

    private final QuizAttemptService attemptService;
    private final CourseService courseService;

    public InstructorResultsController(QuizAttemptService attemptService, CourseService courseService) {
        this.attemptService = attemptService;
        this.courseService = courseService;
    }

    @GetMapping("/courses/{courseId}/results")
    public List<LearnerResultDto> results(@PathVariable Long courseId) {
        courseService.requireOwned(courseId); // 403 si pas le propriétaire (ni ADMIN)
        return attemptService.courseResults(courseId);
    }
}
