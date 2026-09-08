package com.educa.backend.quiz;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.quiz.dto.AttemptResultDto;
import com.educa.backend.quiz.dto.AttemptSubmission;
import com.educa.backend.quiz.dto.AttemptSummaryDto;
import com.educa.backend.quiz.dto.CourseGradeDto;
import com.educa.backend.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    public QuizAttemptController(QuizAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/quizzes/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResultDto submit(@PathVariable Long id, @Valid @RequestBody AttemptSubmission submission) {
        return attemptService.submit(CurrentUser.id(), id, submission);
    }

    @GetMapping("/quizzes/{id}/attempts/me")
    public List<AttemptSummaryDto> myAttempts(@PathVariable Long id) {
        return attemptService.myAttempts(CurrentUser.id(), id);
    }

    @GetMapping("/courses/{courseId}/grade")
    public CourseGradeDto grade(@PathVariable Long courseId) {
        return attemptService.courseGrade(CurrentUser.id(), courseId);
    }
}
