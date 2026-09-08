package com.educa.backend.ai;

/**
 * Repli utilisé quand {@code educa.ai.enabled=false} ou qu'aucune clé API n'est configurée.
 */
public class DisabledAiAssistant implements AiAssistant {

    static final String MESSAGE = "L'assistant est momentanément indisponible. Réessayez plus tard "
            + "ou consultez le contenu du cours.";

    @Override
    public AiReply ask(String systemPrompt, AiChatRequest request) {
        return AiReply.degraded(MESSAGE);
    }
}
