package com.educa.backend.ai;

import org.springframework.http.HttpStatus;

import com.educa.backend.common.error.ApiException;

/** Repli quand l'IA est désactivée ou sans clé : la génération de questions renvoie {@code 503}. */
public class DisabledQuizGenerator implements QuizGenerator {

    static final String MESSAGE = "La génération de questions par l'IA n'est pas disponible pour le moment";

    @Override
    public GeneratedQuiz generate(String courseContent, int count) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE);
    }
}
