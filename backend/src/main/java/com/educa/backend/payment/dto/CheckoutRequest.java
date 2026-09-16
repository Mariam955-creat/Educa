package com.educa.backend.payment.dto;

import com.educa.backend.payment.PaymentProvider;
import com.educa.backend.payment.SubscriptionPlan;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull SubscriptionPlan plan,
        @NotNull PaymentProvider provider) {
}
