package com.educa.backend.quiz.dto;

import java.math.BigDecimal;

public record AttemptResultDto(
        Long attemptId,
        BigDecimal score,
        boolean passed,
        int correctCount,
        int total,
        // renseignés seulement pour un examen final :
        BigDecimal controlsAverage,
        BigDecimal finalGrade,
        Long certificateId) {
}
