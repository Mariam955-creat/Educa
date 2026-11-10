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

    /** Client partagé par le chatbot et la génération de questions ; créé seulement si l'IA est active. */
    private AnthropicClient client;

    @Bean
    AiAssistant aiAssistant(EducaProperties properties) {
        EducaProperties.Ai ai = properties.ai();
        if (!isActive(ai)) {
            log.info("Chatbot IA désactivé (educa.ai.enabled={}, clé {}présente)",
                    ai.enabled(), StringUtils.hasText(ai.apiKey()) ? "" : "non ");
            return new DisabledAiAssistant();
        }
        log.info("Chatbot IA actif — modèle {}", ai.model());
        return new ClaudeAiAssistant(client(ai), ai.model());
    }

    @Bean
    QuizGenerator quizGenerator(EducaProperties properties) {
        EducaProperties.Ai ai = properties.ai();
        return isActive(ai) ? new ClaudeQuizGenerator(client(ai), ai.model()) : new DisabledQuizGenerator();
    }

    private static boolean isActive(EducaProperties.Ai ai) {
        return ai.enabled() && StringUtils.hasText(ai.apiKey());
    }

    private synchronized AnthropicClient client(EducaProperties.Ai ai) {
        if (client == null) {
            client = AnthropicOkHttpClient.builder()
                    .apiKey(ai.apiKey())
                    .timeout(Duration.ofMillis(ai.timeoutMs()))
                    .build();
        }
        return client;
    }
}
