package com.educa.backend.payment.dto;

// Abonnement : courseId est null, courseTitle vaut « Abonnement » et accessUntil la fin de l'accès accordé.

import java.math.BigDecimal;
import java.time.Instant;

import com.educa.backend.payment.PaymentKind;
import com.educa.backend.payment.PaymentProvider;
import com.educa.backend.payment.PaymentStatus;

public record InvoiceDto(
        Long id,
        String invoiceNumber,
        Long courseId,
        String courseTitle,
        PaymentKind kind,
        Instant accessUntil,
        PaymentProvider provider,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt) {
}
