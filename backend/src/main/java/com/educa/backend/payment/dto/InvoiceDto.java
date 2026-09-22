package com.educa.backend.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.educa.backend.payment.PaymentProvider;
import com.educa.backend.payment.PaymentStatus;

public record InvoiceDto(
        Long id,
        String invoiceNumber,
        String courseTitle,
        PaymentProvider provider,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt) {
}
