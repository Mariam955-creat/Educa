package com.educa.backend.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.educa.backend.payment.PaymentProvider;
import com.educa.backend.payment.PaymentStatus;
import com.educa.backend.payment.SubscriptionPlan;

public record AdminPaymentDto(
        Long id,
        String userName,
        PaymentProvider provider,
        SubscriptionPlan plan,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt) {
}
