package com.educa.backend.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.config.EducaProperties;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

/** Europe : cartes, Apple Pay / Google Pay, SEPA — géré par Stripe Checkout (mode {@code payment}, achat unique). */
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    private final StripeClient client;
    private final EducaProperties.Payment.Stripe config;
    private final String publicBaseUrl;

    public StripePaymentGateway(StripeClient client, EducaProperties.Payment.Stripe config, String publicBaseUrl) {
        this.client = client;
        this.config = config;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public CheckoutResult startCheckout(Long userId, String userEmail, Long courseId, String courseSlug,
            String courseTitle, BigDecimal amount) {
        String currency = config.currency();
        long amountCents = amount.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).longValueExact();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(userEmail)
                .setSuccessUrl(publicBaseUrl + "/courses/" + courseSlug + "?payment=success")
                .setCancelUrl(publicBaseUrl + "/courses/" + courseSlug + "?payment=cancelled")
                .putMetadata("userId", userId.toString())
                .putMetadata("courseId", courseId.toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(courseTitle)
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = client.v1().checkout().sessions().create(params);
            return new CheckoutResult(session.getUrl(), session.getId(),
                    BigDecimal.valueOf(amountCents, 2), currency.toUpperCase());
        } catch (StripeException e) {
            log.error("Création de session Stripe Checkout échouée", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Paiement Stripe indisponible pour le moment");
        }
    }

    /** Vérifie la signature puis extrait les données utiles de l'évènement — seul ce module connaît les types Stripe. */
    public StripeEventResult parseWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = com.stripe.net.Webhook.constructEvent(payload, sigHeader, config.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Signature Stripe invalide");
        }

        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if ("checkout.session.completed".equals(event.getType()) && object instanceof Session session) {
            return new StripeEventResult(event.getType(), session.getId());
        }
        return new StripeEventResult(event.getType(), null);
    }

    /** Résultat d'un évènement webhook Stripe, indépendant du SDK — c'est tout ce que {@code PaymentService} reçoit. */
    public record StripeEventResult(String type, String checkoutSessionId) {
    }
}
