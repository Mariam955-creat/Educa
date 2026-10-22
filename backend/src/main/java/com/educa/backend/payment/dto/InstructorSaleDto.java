package com.educa.backend.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.educa.backend.payment.PaymentKind;

/**
 * Vente (paiement réussi) d'un cours du formateur courant : achat à l'unité, ou part d'un pass d'abonnement
 * ({@code kind = SUBSCRIPTION}, {@code amount} = part revenant à ce cours).
 */
public record InstructorSaleDto(Long id, Long courseId, String courseTitle, String buyerName, BigDecimal amount,
                                String currency, Instant createdAt, PaymentKind kind) {
}
