package com.educa.backend.ai;

/**
 * Chatbot pédagogique. Seule implémentation qui connaît le fournisseur LLM :
 * {@link ClaudeAiAssistant}. Repli : {@link DisabledAiAssistant}.
 */
public interface AiAssistant {

    /**
     * @param systemPrompt consignes + contexte du cours (déjà borné en longueur)
     * @param request      question de l'apprenant + historique éventuel
     */
    AiReply ask(String systemPrompt, AiChatRequest request);
}
