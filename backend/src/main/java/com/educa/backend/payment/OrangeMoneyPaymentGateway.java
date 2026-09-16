package com.educa.backend.payment;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.config.EducaProperties;

/**
 * Afrique : intégration <b>directe</b> avec Orange Money Web Payment (pas d'agrégateur) — un
 * compte marchand Orange Developer pour un pays donné (voir {@code educa.payment.orange-money.country}).
 * ⚠️ Noms de champs/endpoints suivent l'API Orange Money Web Payment v1 telle que documentée
 * publiquement ; à revérifier contre la doc live (developer.orange.com) au moment de brancher de
 * vraies clés — non joignable depuis cette session pour confirmation exacte.
 */
public class OrangeMoneyPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(OrangeMoneyPaymentGateway.class);
    private static final String TOKEN_URL = "https://api.orange.com/oauth/v3/token";

    private final RestClient restClient;
    private final EducaProperties.Payment.OrangeMoney config;
    private final String publicBaseUrl;
    private final String webhookBaseUrl;

    public OrangeMoneyPaymentGateway(RestClient restClient, EducaProperties.Payment.OrangeMoney config,
                                     String publicBaseUrl, String webhookBaseUrl) {
        this.restClient = restClient;
        this.config = config;
        this.publicBaseUrl = publicBaseUrl;
        this.webhookBaseUrl = webhookBaseUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CheckoutResult startCheckout(Long userId, String userEmail, SubscriptionPlan plan) {
        int amount = plan == SubscriptionPlan.MONTHLY ? config.monthlyAmount() : config.annualAmount();
        String orderId = "educa-" + userId + "-" + UUID.randomUUID();
        String accessToken = fetchAccessToken();

        Map<String, Object> body = Map.of(
                "merchant_key", config.merchantKey(),
                "currency", config.currency(),
                "order_id", orderId,
                "amount", amount,
                "return_url", publicBaseUrl + "/billing/success",
                "cancel_url", publicBaseUrl + "/billing/cancel",
                "notif_url", webhookBaseUrl + "/api/v1/payments/webhooks/orange-money",
                "lang", "fr",
                "reference", "Abonnement educa — " + (plan == SubscriptionPlan.MONTHLY ? "mensuel" : "annuel"));

        try {
            Map<String, Object> response = restClient.post()
                    .uri("https://api.orange.com/orange-money-webpay/{country}/v1/webpayment", config.country())
                    .headers(h -> h.setBearerAuth(accessToken))
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String paymentUrl = response == null ? null : (String) response.get("payment_url");
            if (paymentUrl == null) {
                log.error("Réponse Orange Money sans payment_url : {}", response);
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Paiement Orange Money indisponible pour le moment");
            }
            return new CheckoutResult(paymentUrl, orderId, BigDecimal.valueOf(amount), config.currency());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Initialisation du paiement Orange Money échouée", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Paiement Orange Money indisponible pour le moment");
        }
    }

    /**
     * Ne jamais faire confiance au corps de la notification webhook seul : on revérifie
     * systématiquement le statut auprès de l'API Orange Money avant de confirmer le paiement.
     */
    @SuppressWarnings("unchecked")
    public boolean isTransactionConfirmed(String orderId, long amount, String payToken) {
        try {
            String accessToken = fetchAccessToken();
            Map<String, Object> response = restClient.get()
                    .uri("https://api.orange.com/orange-money-webpay/{country}/v1/transactionstatus?order_id={orderId}&amount={amount}&pay_token={payToken}",
                            config.country(), orderId, amount, payToken)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .body(Map.class);
            String status = response == null ? null : String.valueOf(response.get("status"));
            return "SUCCESS".equalsIgnoreCase(status);
        } catch (Exception e) {
            log.error("Vérification du statut Orange Money {} échouée", orderId, e);
            return false;
        }
    }

    @Override
    public void cancelAtPeriodEnd(Subscription subscription) {
        // Pas de prélèvement récurrent côté Orange Money : rien à annuler, l'accès expire
        // naturellement à `current_period_end` si aucun nouveau paiement n'est effectué avant.
    }

    @SuppressWarnings("unchecked")
    private String fetchAccessToken() {
        String basicAuth = Base64.getEncoder().encodeToString(
                (config.clientId() + ":" + config.clientSecret()).getBytes(StandardCharsets.UTF_8));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        String token = response == null ? null : (String) response.get("access_token");
        if (token == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Authentification Orange Money indisponible pour le moment");
        }
        return token;
    }
}
