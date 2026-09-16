package com.educa.backend.payment;

/**
 * Prestataire de paiement. Seules {@link StripePaymentGateway} (Europe) et
 * {@link OrangeMoneyPaymentGateway} (Afrique) connaissent leur API respective ;
 * repli {@link DisabledPaymentGateway} si la clé du prestataire est absente
 * (même patron que {@code AiAssistant}/{@code DisabledAiAssistant}).
 */
public interface PaymentGateway {

    /** Démarre un paiement d'abonnement, renvoie l'URL de paiement hébergée à laquelle rediriger l'utilisateur. */
    CheckoutResult startCheckout(Long userId, String userEmail, SubscriptionPlan plan);

    /**
     * Empêche le renouvellement automatique à la fin de la période en cours.
     * Sans effet côté prestataires sans prélèvement récurrent natif (Orange Money).
     */
    void cancelAtPeriodEnd(Subscription subscription);

    record CheckoutResult(String checkoutUrl, String providerReference, java.math.BigDecimal amount, String currency) {
    }
}
