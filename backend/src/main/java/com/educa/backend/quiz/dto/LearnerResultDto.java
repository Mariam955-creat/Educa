package com.educa.backend.quiz.dto;

import java.math.BigDecimal;

public record LearnerResultDto(
        Long userId,
        String learnerName,
        BigDecimal controlsAverage,
        BigDecimal finalExamBestScore,
        BigDecimal finalGrade,
        boolean certified) {
}
