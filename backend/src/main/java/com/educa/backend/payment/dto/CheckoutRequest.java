package com.educa.backend.payment.dto;

import com.educa.backend.payment.PaymentProvider;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull PaymentProvider provider) {
}
