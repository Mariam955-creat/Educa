package com.educa.backend.ai;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;

/**
 * Implémentation du chatbot via l'API Claude (SDK officiel Anthropic).
 * Toute erreur ou timeout est convertie en réponse « dégradée » — jamais d'exception vers l'appelant.
 */
public class ClaudeAiAssistant implements AiAssistant {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiAssistant.class);
    /**
     * Les modèles récents (Sonnet 5…) réfléchissent avant de répondre, et cette réflexion compte dans
     * {@code max_tokens} : à 1024, elle pouvait tout consommer et laisser une réponse vide.
     */
    private static final long MAX_TOKENS = 4096L;
    /** Effort bas : questions de chat courtes, réponse rapide (sous {@code AI_TIMEOUT_MS}) et moins de tokens. */
    private static final OutputConfig.Effort EFFORT = OutputConfig.Effort.LOW;

    private final AnthropicClient client;
    private final String model;

    public ClaudeAiAssistant(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public AiReply ask(String systemPrompt, AiChatRequest request) {
        try {
            MessageCreateParams.Builder builder = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(MAX_TOKENS)
                    .outputConfig(OutputConfig.builder().effort(EFFORT).build())
                    .system(systemPrompt);

            if (request.history() != null) {
                for (AiChatRequest.Turn turn : request.history()) {
                    if (turn == null || turn.content() == null || turn.content().isBlank()) {
                        continue;
                    }
                    if ("assistant".equalsIgnoreCase(turn.role())) {
                        builder.addAssistantMessage(turn.content());
                    } else {
                        builder.addUserMessage(turn.content());
                    }
                }
            }
            builder.addUserMessage(request.message());

            Message response = client.messages().create(builder.build());
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(block -> block.text())
                    .collect(Collectors.joining("\n"))
                    .trim();

            if (text.isBlank()) {
                log.warn("Réponse du chatbot sans texte (stop_reason={})", response.stopReason().orElse(null));
                return AiReply.degraded(DisabledAiAssistant.MESSAGE);
            }
            return AiReply.ok(text);
        } catch (RuntimeException ex) {
            log.warn("Appel au chatbot échoué : {}", ex.getMessage());
            return AiReply.degraded(DisabledAiAssistant.MESSAGE);
        }
    }
}
