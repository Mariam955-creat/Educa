package com.educa.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration applicative regroupée sous le préfixe {@code educa} (voir application.yml).
 */
@ConfigurationProperties(prefix = "educa")
public record EducaProperties(
        Cors cors,
        Jwt jwt,
        Storage storage,
        Ai ai,
        Payment payment,
        String publicBaseUrl) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlSeconds) {
    }

    public record Storage(String localPath, int maxFileSizeMb) {
    }

    public record Ai(boolean enabled, String apiKey, String model, int timeoutMs, int maxContextChars) {
    }

    /**
     * {@code webhookBaseUrl} : origine où l'API est joignable par les prestataires (callbacks
     * serveur-à-serveur) — distincte de {@code publicBaseUrl} (origine du frontend, pour les
     * redirections navigateur), les deux diffèrent en dev (:8081 vs :4200).
     */
    public record Payment(String webhookBaseUrl, Stripe stripe, OrangeMoney orangeMoney) {

        public record Stripe(boolean enabled, String secretKey, String webhookSecret,
                             int monthlyAmountCents, int annualAmountCents, String currency) {
        }

        /** Intégration directe (pas d'agrégateur) — un compte marchand Orange Developer, un pays donné. */
        public record OrangeMoney(boolean enabled, String clientId, String clientSecret, String merchantKey,
                                  String country, int monthlyAmount, int annualAmount, String currency) {
        }
    }
}
