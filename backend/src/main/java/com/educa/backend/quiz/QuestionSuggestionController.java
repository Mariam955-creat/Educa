package com.educa.backend.quiz;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.quiz.dto.QuestionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
public class QuestionSuggestionController {

    private final QuestionSuggestionService suggestionService;

    public QuestionSuggestionController(QuestionSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    /**
     * Propose des questions générées par l'IA à partir du contenu du chapitre (contrôle) ou du cours (examen final).
     * Rien n'est enregistré. {@code 503} si l'IA est indisponible.
     */
    @PostMapping("/quizzes/{id}/question-suggestions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public List<QuestionRequest> suggest(@PathVariable Long id, @Valid @RequestBody SuggestionRequest request) {
        return suggestionService.suggest(id, request.count() != null ? request.count() : 5);
    }

    /** @param count nombre de questions souhaité (1 à 10, 5 par défaut) */
    public record SuggestionRequest(@Min(1) @Max(QuestionSuggestionService.MAX_COUNT) Integer count) {
    }
}
