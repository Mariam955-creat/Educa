package com.educa.backend.payment;

import java.math.BigDecimal;

/**
 * Prestataire de paiement. Seules {@link StripePaymentGateway} (Europe) et
 * {@link OrangeMoneyPaymentGateway} (Afrique) connaissent leur API respective ;
 * repli {@link DisabledPaymentGateway} si la clé du prestataire est absente
 * (même patron que {@code AiAssistant}/{@code DisabledAiAssistant}).
 */
public interface PaymentGateway {

    /**
     * Démarre l'achat d'un cours (paiement unique), renvoie l'URL de paiement hébergée à laquelle
     * rediriger l'utilisateur. {@code amount} vient du prix du cours ; la devise est celle configurée
     * pour ce prestataire (voir {@code educa.payment.stripe.currency}/{@code orange-money.currency}).
     */
    CheckoutResult startCheckout(Long userId, String userEmail, Long courseId, String courseSlug, String courseTitle,
            BigDecimal amount);

    record CheckoutResult(String checkoutUrl, String providerReference, BigDecimal amount, String currency) {
    }
}
