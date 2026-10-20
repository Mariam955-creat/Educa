package com.educa.backend.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.educa.backend.payment.PaymentKind;
import com.educa.backend.payment.PaymentProvider;
import com.educa.backend.payment.PaymentStatus;

public record AdminPaymentDto(
        Long id,
        String userName,
        String courseTitle,
        PaymentKind kind,
        PaymentProvider provider,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String invoiceNumber,
        Instant createdAt) {
}
