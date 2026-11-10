package com.educa.backend.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Questions proposées par l'IA (sortie structurée : le schéma JSON envoyé au modèle est dérivé de ces records).
 * Ce sont des brouillons : le formateur les relit et choisit lesquelles ajouter à son quiz.
 */
public record GeneratedQuiz(
        @JsonPropertyDescription("Les questions proposées") List<GeneratedQuestion> questions) {

    public enum Kind {
        SINGLE_CHOICE,
        MULTIPLE_CHOICE,
        TRUE_FALSE
    }

    public record GeneratedQuestion(
            @JsonPropertyDescription("Énoncé de la question, autonome et sans ambiguïté") String statement,
            @JsonPropertyDescription("SINGLE_CHOICE : une seule bonne réponse ; MULTIPLE_CHOICE : plusieurs ; "
                    + "TRUE_FALSE : exactement deux options, Vrai et Faux (dans la langue du cours)") Kind type,
            @JsonPropertyDescription("Options de réponse, de 2 à 5") List<GeneratedOption> options) {
    }

    public record GeneratedOption(
            @JsonPropertyDescription("Texte de l'option") String label,
            @JsonPropertyDescription("true si l'option est une bonne réponse") boolean correct) {
    }
}
