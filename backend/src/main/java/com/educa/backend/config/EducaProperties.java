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
        Ai ai) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlSeconds) {
    }

    public record Storage(String localPath, int maxFileSizeMb) {
    }

    public record Ai(boolean enabled, String apiKey, String model, int timeoutMs, int maxContextChars) {
    }
}
