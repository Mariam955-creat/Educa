package com.educa.backend.ai;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.educa.backend.config.EducaProperties;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    AiAssistant aiAssistant(EducaProperties properties) {
        EducaProperties.Ai ai = properties.ai();
        if (!ai.enabled() || !StringUtils.hasText(ai.apiKey())) {
            log.info("Chatbot IA désactivé (educa.ai.enabled={}, clé {}présente)",
                    ai.enabled(), StringUtils.hasText(ai.apiKey()) ? "" : "non ");
            return new DisabledAiAssistant();
        }
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(ai.apiKey())
                .timeout(Duration.ofMillis(ai.timeoutMs()))
                .build();
        log.info("Chatbot IA actif — modèle {}", ai.model());
        return new ClaudeAiAssistant(client, ai.model());
    }
}
