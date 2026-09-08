package com.educa.backend.quiz;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.quiz.dto.QuestionRequest;
import com.educa.backend.quiz.dto.QuizRequest;
import com.educa.backend.quiz.dto.QuizViewDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // ---------- création / édition (formateur propriétaire / ADMIN) ----------

    @PostMapping("/chapters/{chapterId}/control-quiz")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizViewDto createControl(@PathVariable Long chapterId, @Valid @RequestBody QuizRequest request) {
        return quizService.createControlQuiz(chapterId, request);
    }

    @PostMapping("/courses/{courseId}/final-exam")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizViewDto createFinalExam(@PathVariable Long courseId, @Valid @RequestBody QuizRequest request) {
        return quizService.createFinalExam(courseId, request);
    }

    @PutMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public QuizViewDto update(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        return quizService.updateQuiz(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        quizService.deleteQuiz(id);
    }

    @PostMapping("/quizzes/{id}/questions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public QuizViewDto addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return quizService.addQuestion(id, request);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public QuizViewDto updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return quizService.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
    }

    // ---------- lecture (apprenant inscrit, ou propriétaire/ADMIN) ----------

    @GetMapping("/quizzes/{id}")
    public QuizViewDto view(@PathVariable Long id) {
        return quizService.getViewForCurrentUser(id);
    }

    @GetMapping("/courses/{courseId}/quizzes")
    public com.educa.backend.quiz.dto.CourseQuizzesDto courseQuizzes(@PathVariable Long courseId) {
        return quizService.listForCourse(courseId);
    }
}
