package com.educa.backend.ai;

import java.time.Instant;

/**
 * Un message de l'historique du chatbot.
 *
 * @param role     {@code user} ou {@code assistant} (même forme que {@link AiChatRequest.Turn})
 * @param degraded réponse de repli (IA indisponible)
 */
public record ChatMessageDto(String role, String content, boolean degraded, Instant createdAt) {

    static ChatMessageDto of(ChatMessage message) {
        return new ChatMessageDto(message.getRole().name().toLowerCase(), message.getContent(),
                message.isDegraded(), message.getCreatedAt());
    }
}
