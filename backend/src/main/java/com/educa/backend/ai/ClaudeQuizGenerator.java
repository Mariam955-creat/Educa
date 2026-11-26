package com.educa.backend.ai;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.educa.backend.common.error.ApiException;

/**
 * Génération de questions via l'API Claude, en sortie structurée : le modèle répond directement au format
 * {@link GeneratedQuiz} (schéma JSON dérivé des records), sans analyse de texte libre.
 */
public class ClaudeQuizGenerator implements QuizGenerator {

    private static final Logger log = LoggerFactory.getLogger(ClaudeQuizGenerator.class);
    private static final long MAX_TOKENS = 16000L;
    /** Plusieurs questions à rédiger : bien plus long qu'une réponse de chat, d'où un délai propre. */
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private static final String SYSTEM = """
            Tu aides un formateur de la plateforme e-learning Omnia à préparer un quiz.
            À partir du contenu de cours fourni, propose des questions qui vérifient la compréhension des notions
            importantes du cours, pas des détails anecdotiques.
            Règles :
            - Chaque question porte uniquement sur ce qui est dans le contenu fourni ; n'invente rien.
            - Rédige les questions et les options dans la langue du cours (indiquée en tête du contenu).
            - Varie les types : surtout des questions à choix unique, quelques questions à choix multiple et Vrai/Faux.
            - Choix unique : exactement une bonne réponse. Choix multiple : au moins deux bonnes réponses.
            - Vrai/Faux : exactement deux options, « Vrai » et « Faux » traduits dans la langue du cours.
            - Les mauvaises réponses doivent être plausibles pour quelqu'un qui n'a pas suivi le cours.
            - Pas de question piège ni de formulation négative ambiguë.
            """;

    private final AnthropicClient client;
    private final String model;

    public ClaudeQuizGenerator(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public GeneratedQuiz generate(String courseContent, int count) {
        StructuredMessageCreateParams<GeneratedQuiz> params = params(courseContent, count);
        StructuredMessage<GeneratedQuiz> response;
        try {
            response = client.withOptions(options -> options.timeout(TIMEOUT)).messages().create(params);
        } catch (RuntimeException ex) {
            log.warn("Génération de questions échouée : {}", ex.getMessage());
            throw unavailable();
        }
        return read(response);
    }

    /** Requête au modèle ; le SDK dérive et valide le schéma JSON de {@link GeneratedQuiz} dès sa construction. */
    StructuredMessageCreateParams<GeneratedQuiz> params(String courseContent, int count) {
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM)
                .addUserMessage("Propose exactement " + count + " questions sur ce contenu.\n\n"
                        + "--- CONTENU DU COURS ---\n" + courseContent + "\n--- FIN DU CONTENU ---")
                .outputConfig(GeneratedQuiz.class)
                .build();
    }

    private static GeneratedQuiz read(StructuredMessage<GeneratedQuiz> response) {
        StopReason stop = response.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(stop) || StopReason.MAX_TOKENS.equals(stop)) {
            log.warn("Génération de questions interrompue (stop_reason={})", stop);
            throw unavailable();
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(block -> block.text())
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Génération de questions sans contenu");
                    return unavailable();
                });
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "La génération de questions a échoué. Réessayez dans un instant.");
    }
}
