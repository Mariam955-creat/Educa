package com.educa.backend.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.educa.backend.common.error.ApiException;

/** Sans clé Anthropic (profil {@code test}), la génération de questions répond {@code 503}, sans repli. */
class DisabledQuizGeneratorTest {

    @Test
    void sans_ia_la_generation_renvoie_503() {
        assertThatThrownBy(() -> new DisabledQuizGenerator().generate("contenu", 5))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
