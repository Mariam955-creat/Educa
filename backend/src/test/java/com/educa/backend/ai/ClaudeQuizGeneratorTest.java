package com.educa.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredMessageCreateParams;

/**
 * Construction de la requête de génération, sans appel réseau : le SDK dérive le schéma JSON de
 * {@link GeneratedQuiz} et le valide localement (types acceptés par les sorties structurées) dès {@code build()}.
 */
class ClaudeQuizGeneratorTest {

    @Test
    void la_requete_porte_le_schema_des_questions_et_le_contenu_du_cours() {
        ClaudeQuizGenerator generator = new ClaudeQuizGenerator(
                AnthropicOkHttpClient.builder().apiKey("cle-de-test").build(), "claude-sonnet-5");

        StructuredMessageCreateParams<GeneratedQuiz> params = generator.params("Les variables Python", 4);

        OutputConfig output = params.rawParams().outputConfig().orElseThrow();
        assertThat(output.format()).isPresent();
        assertThat(output.format().orElseThrow().toString())
                .contains("questions", "statement", "options", "correct", "SINGLE_CHOICE", "TRUE_FALSE");
        assertThat(params.rawParams().toString()).contains("Propose exactement 4 questions", "Les variables Python");
    }
}
