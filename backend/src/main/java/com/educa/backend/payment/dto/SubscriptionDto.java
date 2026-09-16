package com.educa.backend.payment.dto;

import java.time.Instant;

import com.educa.backend.payment.PaymentProvider;
import com.educa.backend.payment.SubscriptionPlan;
import com.educa.backend.payment.SubscriptionStatus;

public record SubscriptionDto(
        boolean hasSubscription,
        SubscriptionPlan plan,
        PaymentProvider provider,
        SubscriptionStatus status,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        boolean active) {

    public static SubscriptionDto none() {
        return new SubscriptionDto(false, null, null, null, null, false, false);
    }
}
