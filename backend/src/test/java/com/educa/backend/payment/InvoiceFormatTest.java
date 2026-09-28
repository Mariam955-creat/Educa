package com.educa.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class InvoiceFormatTest {

    @Test
    void montant_en_euros_au_format_francais() {
        assertThat(PaymentService.formatAmount(new BigDecimal("19.99"), "EUR", Locale.FRANCE)).isEqualTo("19,99 €");
        assertThat(PaymentService.formatAmount(new BigDecimal("1250.5"), "eur", Locale.FRANCE)).isEqualTo("1 250,50 €");
    }

    @Test
    void montant_en_franc_cfa_sans_decimales() {
        assertThat(PaymentService.formatAmount(new BigDecimal("20000"), "XOF", Locale.FRANCE)).doesNotContain(" ")
                .startsWith("20 000");
    }

    @Test
    void montant_suit_la_langue() {
        assertThat(PaymentService.formatAmount(new BigDecimal("19.99"), "EUR", Locale.UK)).isEqualTo("€19.99");
        assertThat(PaymentService.formatAmount(new BigDecimal("19.99"), "EUR", Locale.forLanguageTag("nl-NL")))
                .contains("19,99");
    }
}
