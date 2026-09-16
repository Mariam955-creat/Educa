package com.educa.backend.payment;

import java.time.Instant;
import java.util.List;

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
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

/** Europe : cartes, Apple Pay / Google Pay, SEPA — géré par Stripe Checkout (mode {@code subscription}). */
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
    public CheckoutResult startCheckout(Long userId, String userEmail, SubscriptionPlan plan) {
        long amountCents = plan == SubscriptionPlan.MONTHLY ? config.monthlyAmountCents() : config.annualAmountCents();
        SessionCreateParams.LineItem.PriceData.Recurring.Interval interval = plan == SubscriptionPlan.MONTHLY
                ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH
                : SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(userEmail)
                .setSuccessUrl(publicBaseUrl + "/billing/success")
                .setCancelUrl(publicBaseUrl + "/billing/cancel")
                .putMetadata("userId", userId.toString())
                .putMetadata("plan", plan.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(config.currency())
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Abonnement educa — " + (plan == SubscriptionPlan.MONTHLY ? "mensuel" : "annuel"))
                                        .build())
                                .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                        .setInterval(interval)
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = client.v1().checkout().sessions().create(params);
            return new CheckoutResult(session.getUrl(), session.getId(),
                    java.math.BigDecimal.valueOf(amountCents, 2), config.currency().toUpperCase());
        } catch (StripeException e) {
            log.error("Création de session Stripe Checkout échouée", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Paiement Stripe indisponible pour le moment");
        }
    }

    @Override
    public void cancelAtPeriodEnd(Subscription subscription) {
        if (subscription.getProviderSubscriptionId() == null) {
            return;
        }
        try {
            client.v1().subscriptions().update(subscription.getProviderSubscriptionId(),
                    SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build());
        } catch (StripeException e) {
            log.error("Annulation de l'abonnement Stripe {} échouée", subscription.getProviderSubscriptionId(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Annulation Stripe indisponible pour le moment");
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
        return switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = (Session) object;
                String subscriptionId = session.getSubscription();
                Instant periodEnd = subscriptionId != null ? fetchPeriodEnd(subscriptionId) : null;
                yield new StripeEventResult(event.getType(), session.getId(), subscriptionId,
                        session.getCustomer(), periodEnd, "active");
            }
            case "customer.subscription.updated", "customer.subscription.deleted" -> {
                com.stripe.model.Subscription sub = (com.stripe.model.Subscription) object;
                yield new StripeEventResult(event.getType(), null, sub.getId(), sub.getCustomer(),
                        periodEndOf(sub), sub.getStatus());
            }
            default -> new StripeEventResult(event.getType(), null, null, null, null, null);
        };
    }

    private Instant fetchPeriodEnd(String subscriptionId) {
        try {
            return periodEndOf(client.v1().subscriptions().retrieve(subscriptionId));
        } catch (StripeException e) {
            log.error("Lecture de l'abonnement Stripe {} échouée", subscriptionId, e);
            return null;
        }
    }

    private static Instant periodEndOf(com.stripe.model.Subscription sub) {
        List<SubscriptionItem> items = sub.getItems().getData();
        if (items.isEmpty() || items.get(0).getCurrentPeriodEnd() == null) {
            return null;
        }
        return Instant.ofEpochSecond(items.get(0).getCurrentPeriodEnd());
    }

    /** Résultat d'un évènement webhook Stripe, indépendant du SDK — c'est tout ce que {@code SubscriptionService} reçoit. */
    public record StripeEventResult(String type, String checkoutSessionId, String subscriptionId,
                                    String customerId, Instant periodEnd, String status) {
    }
}
