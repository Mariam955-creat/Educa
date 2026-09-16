package com.educa.backend.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.educa.backend.config.EducaProperties;
import com.stripe.StripeClient;

/**
 * Fabrique les deux passerelles de paiement. Repli {@link DisabledPaymentGateway} si la clé du
 * prestataire est absente (même patron que {@code AiConfig}/{@code DisabledAiAssistant}) — le bean
 * existe toujours, il répond juste "indisponible" plutôt que d'empêcher le démarrage de l'appli.
 */
@Configuration
public class PaymentConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);

    @Bean("stripeGateway")
    PaymentGateway stripeGateway(EducaProperties properties) {
        EducaProperties.Payment.Stripe stripe = properties.payment().stripe();
        if (!stripe.enabled() || !StringUtils.hasText(stripe.secretKey())) {
            log.info("Paiement Stripe désactivé (educa.payment.stripe.enabled={}, clé {}présente)",
                    stripe.enabled(), StringUtils.hasText(stripe.secretKey()) ? "" : "non ");
            return new DisabledPaymentGateway("Stripe");
        }
        log.info("Paiement Stripe actif");
        return new StripePaymentGateway(new StripeClient(stripe.secretKey()), stripe, properties.publicBaseUrl());
    }

    @Bean("orangeMoneyGateway")
    PaymentGateway orangeMoneyGateway(EducaProperties properties) {
        EducaProperties.Payment.OrangeMoney orangeMoney = properties.payment().orangeMoney();
        if (!orangeMoney.enabled() || !StringUtils.hasText(orangeMoney.clientId())) {
            log.info("Paiement Orange Money désactivé (educa.payment.orange-money.enabled={}, clé {}présente)",
                    orangeMoney.enabled(), StringUtils.hasText(orangeMoney.clientId()) ? "" : "non ");
            return new DisabledPaymentGateway("Orange Money");
        }
        log.info("Paiement Orange Money actif (pays {})", orangeMoney.country());
        return new OrangeMoneyPaymentGateway(RestClient.create(), orangeMoney, properties.publicBaseUrl(),
                properties.payment().webhookBaseUrl());
    }
}
