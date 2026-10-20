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
     * Démarre un paiement unique (achat d'un cours ou pass d'abonnement), renvoie l'URL de paiement hébergée à
     * laquelle rediriger l'utilisateur. {@code courseId} est {@code null} pour un abonnement ; {@code itemName} est le
     * libellé affiché par le prestataire ; {@code returnPath} est la page du frontend où revenir (suivie de
     * {@code ?payment=success|cancelled}). La devise est celle configurée pour ce prestataire (voir
     * {@code educa.payment.stripe.currency}/{@code orange-money.currency}).
     */
    CheckoutResult startCheckout(Long userId, String userEmail, Long courseId, String itemName, BigDecimal amount,
            String returnPath);

    record CheckoutResult(String checkoutUrl, String providerReference, BigDecimal amount, String currency) {
    }
}
