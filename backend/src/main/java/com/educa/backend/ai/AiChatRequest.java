package com.educa.backend.ai;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param history échanges précédents transmis à {@link AiAssistant}. Dans une requête HTTP, ce champ est
 *                accepté (compatibilité) mais ignoré : {@link AiChatService} le remplace par l'historique
 *                enregistré côté serveur.
 */
public record AiChatRequest(
        @NotNull Long courseId,
        @NotNull @Size(min = 1, max = 2000) String message,
        List<Turn> history) {

    /** Un échange précédent : {@code role} = {@code "user"} ou {@code "assistant"}. */
    public record Turn(String role, String content) {
    }
}
