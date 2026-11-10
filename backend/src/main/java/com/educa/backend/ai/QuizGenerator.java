package com.educa.backend.ai;

/**
 * Génère des questions de quiz à partir du contenu d'un cours. Implémentations : {@link ClaudeQuizGenerator}
 * (API Claude) et {@link DisabledQuizGenerator} (IA désactivée). Contrairement au chatbot, il n'y a pas de
 * réponse de repli : une génération impossible lève une {@code ApiException} {@code 503}.
 */
public interface QuizGenerator {

    /**
     * @param courseContent contenu du cours ou du chapitre (déjà borné en longueur)
     * @param count         nombre de questions souhaité
     */
    GeneratedQuiz generate(String courseContent, int count);
}
