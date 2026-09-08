package com.educa.backend.ai;

/**
 * Réponse du chatbot. {@code degraded = true} signale un repli
 * (IA désactivée, clé absente, timeout ou erreur d'appel).
 */
public record AiReply(String reply, boolean degraded) {

    public static AiReply degraded(String reply) {
        return new AiReply(reply, true);
    }

    public static AiReply ok(String reply) {
        return new AiReply(reply, false);
    }
}
