package com.educa.backend.payment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.payment.dto.AdminPaymentDto;
import com.educa.backend.payment.dto.CheckoutRequest;
import com.educa.backend.payment.dto.CheckoutResponse;
import com.educa.backend.payment.dto.SubscriptionDto;
import com.educa.backend.user.UserService;
import com.educa.backend.user.dto.UserDto;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final PaymentGateway stripeGateway;
    private final PaymentGateway orangeMoneyGateway;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, PaymentRepository paymentRepository,
                               UserService userService,
                               @Qualifier("stripeGateway") PaymentGateway stripeGateway,
                               @Qualifier("orangeMoneyGateway") PaymentGateway orangeMoneyGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.userService = userService;
        this.stripeGateway = stripeGateway;
        this.orangeMoneyGateway = orangeMoneyGateway;
    }

    @Transactional
    public CheckoutResponse startCheckout(Long userId, CheckoutRequest request) {
        UserDto user = userService.getById(userId);
        PaymentGateway.CheckoutResult result = gatewayFor(request.provider())
                .startCheckout(userId, user.email(), request.plan());

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setProvider(request.provider());
        payment.setProviderReference(result.providerReference());
        payment.setPlan(request.plan());
        payment.setAmount(result.amount());
        payment.setCurrency(result.currency());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return new CheckoutResponse(result.checkoutUrl());
    }

    /** Apprenants : nécessite un abonnement actif. Formateurs/admins : toujours autorisés (voir EnrollmentService). */
    @Transactional(readOnly = true)
    public boolean hasActiveAccess(Long userId) {
        UserDto user = userService.getById(userId);
        if (user.roles().contains("INSTRUCTOR") || user.roles().contains("ADMIN")) {
            return true;
        }
        return subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(SubscriptionService::isActive)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public SubscriptionDto mySubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(s -> new SubscriptionDto(true, s.getPlan(), s.getProvider(), s.getStatus(),
                        s.getCurrentPeriodEnd(), s.isCancelAtPeriodEnd(), isActive(s)))
                .orElseGet(SubscriptionDto::none);
    }

    /**
     * Seul un statut {@code ACTIVE} avec une période en cours donne accès — un statut {@code EXPIRED}
     * (ex. Stripe "past_due") ne doit jamais rouvrir l'accès même si `current_period_end` n'a pas
     * encore été recalculé.
     */
    private static boolean isActive(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(Instant.now());
    }

    @Transactional
    public void cancel(Long userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun abonnement actif"));
        gatewayFor(subscription.getProvider()).cancelAtPeriodEnd(subscription);
        subscription.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public Page<AdminPaymentDto> registry(Pageable pageable) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> new AdminPaymentDto(p.getId(), userService.displayNameById(p.getUserId()), p.getProvider(),
                        p.getPlan(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getCreatedAt()));
    }

    // ---------- webhooks ----------

    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        if (!(stripeGateway instanceof StripePaymentGateway stripe)) {
            log.warn("Webhook Stripe reçu mais Stripe n'est pas configuré, ignoré");
            return;
        }
        StripePaymentGateway.StripeEventResult result = stripe.parseWebhook(payload, sigHeader);
        switch (result.type()) {
            case "checkout.session.completed" -> confirmStripeCheckout(result);
            case "customer.subscription.updated", "customer.subscription.deleted" -> syncStripeSubscription(result);
            default -> log.debug("Évènement Stripe ignoré : {}", result.type());
        }
    }

    private void confirmStripeCheckout(StripePaymentGateway.StripeEventResult result) {
        Payment payment = paymentRepository
                .findByProviderAndProviderReference(PaymentProvider.STRIPE, result.checkoutSessionId())
                .orElseThrow(() -> {
                    log.warn("Webhook Stripe : session {} introuvable localement, nouvelle tentative demandée",
                            result.checkoutSessionId());
                    return new ApiException(HttpStatus.CONFLICT, "Paiement local pas encore visible, réessayer");
                });
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);
        // Si Stripe n'a pas pu être rappelé pour lire la période exacte (panne transitoire), on ne
        // laisse jamais un abonnement payé sans accès : période estimée à partir de la formule,
        // corrigée dès le prochain évènement customer.subscription.updated.
        Instant periodEnd = result.periodEnd() != null
                ? result.periodEnd()
                : Instant.now().plus(payment.getPlan() == SubscriptionPlan.MONTHLY ? 30 : 365, ChronoUnit.DAYS);
        upsertSubscription(payment.getUserId(), payment.getPlan(), PaymentProvider.STRIPE,
                result.subscriptionId(), result.customerId(), periodEnd, SubscriptionStatus.ACTIVE);
    }

    private void syncStripeSubscription(StripePaymentGateway.StripeEventResult result) {
        Subscription sub = subscriptionRepository.findByProviderSubscriptionId(result.subscriptionId())
                .orElseThrow(() -> {
                    log.warn("Webhook Stripe : abonnement {} introuvable localement, nouvelle tentative demandée",
                            result.subscriptionId());
                    return new ApiException(HttpStatus.CONFLICT, "Abonnement local pas encore visible, réessayer");
                });
        sub.setStatus(mapStripeStatus(result.status()));
        if (result.periodEnd() != null) {
            sub.setCurrentPeriodEnd(result.periodEnd());
        }
        subscriptionRepository.save(sub);
    }

    private static SubscriptionStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return SubscriptionStatus.ACTIVE;
        }
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "canceled", "unpaid" -> SubscriptionStatus.CANCELLED;
            default -> SubscriptionStatus.EXPIRED;
        };
    }

    /** Orange Money : sans prélèvement récurrent, chaque paiement confirmé prolonge simplement la période en cours. */
    @Transactional
    public void handleOrangeMoneyWebhook(String orderId, long amount, String payToken) {
        if (!(orangeMoneyGateway instanceof OrangeMoneyPaymentGateway orangeMoney)) {
            log.warn("Webhook Orange Money reçu mais Orange Money n'est pas configuré, ignoré");
            return;
        }
        Payment payment = paymentRepository
                .findByProviderAndProviderReference(PaymentProvider.ORANGE_MONEY, orderId)
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook Orange Money : transaction {} introuvable localement, ignoré", orderId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        // Le montant du webhook doit correspondre à celui enregistré au moment du checkout — sinon on
        // ne fait jamais confiance à la seule confirmation de statut du prestataire pour ce montant.
        if (payment.getAmount().longValueExact() != amount) {
            log.warn("Webhook Orange Money : montant {} ne correspond pas au paiement {} attendu ({}), rejeté",
                    amount, orderId, payment.getAmount());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return;
        }
        if (!orangeMoney.isTransactionConfirmed(orderId, amount, payToken)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return;
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        Instant base = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(payment.getUserId())
                .filter(s -> s.getProvider() == PaymentProvider.ORANGE_MONEY
                        && s.getCurrentPeriodEnd() != null && s.getCurrentPeriodEnd().isAfter(Instant.now()))
                .map(Subscription::getCurrentPeriodEnd)
                .orElse(Instant.now());
        Instant periodEnd = base.plus(payment.getPlan() == SubscriptionPlan.MONTHLY ? 30 : 365, ChronoUnit.DAYS);

        upsertSubscription(payment.getUserId(), payment.getPlan(), PaymentProvider.ORANGE_MONEY,
                null, null, periodEnd, SubscriptionStatus.ACTIVE);
    }

    // ---------- privé ----------

    private void upsertSubscription(Long userId, SubscriptionPlan plan, PaymentProvider provider,
                                    String providerSubscriptionId, String providerCustomerId,
                                    Instant periodEnd, SubscriptionStatus status) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(s -> s.getProvider() == provider && s.getStatus() != SubscriptionStatus.CANCELLED)
                .orElseGet(Subscription::new);
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setProvider(provider);
        subscription.setStatus(status);
        if (providerSubscriptionId != null) {
            subscription.setProviderSubscriptionId(providerSubscriptionId);
        }
        if (providerCustomerId != null) {
            subscription.setProviderCustomerId(providerCustomerId);
        }
        if (periodEnd != null) {
            subscription.setCurrentPeriodEnd(periodEnd);
        }
        subscriptionRepository.save(subscription);
    }

    private PaymentGateway gatewayFor(PaymentProvider provider) {
        return provider == PaymentProvider.STRIPE ? stripeGateway : orangeMoneyGateway;
    }
}
