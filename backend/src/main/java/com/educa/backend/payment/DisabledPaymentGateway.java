package com.educa.backend.payment;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

import com.educa.backend.common.error.ApiException;

/** Repli quand la clé du prestataire est absente/désactivée — paiement indisponible, pas de crash au démarrage. */
public class DisabledPaymentGateway implements PaymentGateway {

    private final String providerLabel;

    public DisabledPaymentGateway(String providerLabel) {
        this.providerLabel = providerLabel;
    }

    @Override
    public CheckoutResult startCheckout(Long userId, String userEmail, Long courseId, String courseSlug,
            String courseTitle, BigDecimal amount) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Paiement " + providerLabel + " indisponible pour le moment");
    }
}
