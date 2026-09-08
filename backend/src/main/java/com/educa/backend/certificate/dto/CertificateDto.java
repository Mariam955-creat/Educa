package com.educa.backend.certificate.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CertificateDto(
        Long id,
        String serialNumber,
        String verificationCode,
        Long courseId,
        String courseTitle,
        String holderName,
        BigDecimal controlsAverage,
        BigDecimal finalExamScore,
        BigDecimal finalGrade,
        Instant issuedAt) {
}
