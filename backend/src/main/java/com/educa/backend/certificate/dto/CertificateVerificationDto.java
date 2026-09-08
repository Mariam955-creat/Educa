package com.educa.backend.certificate.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CertificateVerificationDto(
        boolean valid,
        String serialNumber,
        String holderName,
        String courseTitle,
        BigDecimal finalGrade,
        Instant issuedAt) {

    public static CertificateVerificationDto invalid() {
        return new CertificateVerificationDto(false, null, null, null, null, null);
    }
}
