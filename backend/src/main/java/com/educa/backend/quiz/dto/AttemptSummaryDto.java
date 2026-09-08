package com.educa.backend.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AttemptSummaryDto(
        Long id,
        BigDecimal score,
        boolean passed,
        Instant submittedAt) {
}
