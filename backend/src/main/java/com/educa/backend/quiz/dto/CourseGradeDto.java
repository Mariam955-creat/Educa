package com.educa.backend.quiz.dto;

import java.math.BigDecimal;
import java.util.List;

public record CourseGradeDto(
        Long courseId,
        int progressPercent,
        boolean finalExamUnlocked,
        BigDecimal controlsAverage,
        List<ControlScoreDto> controls,
        BigDecimal finalExamBestScore,
        BigDecimal finalGrade,
        int passThreshold,
        Long certificateId) {

    public record ControlScoreDto(
            Long quizId,
            Long chapterId,
            BigDecimal bestScore,
            long attempts) {
    }
}
