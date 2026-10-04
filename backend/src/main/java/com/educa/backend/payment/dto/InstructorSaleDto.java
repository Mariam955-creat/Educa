package com.educa.backend.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Vente (paiement réussi) d'un cours du formateur courant. */
public record InstructorSaleDto(Long id, Long courseId, String courseTitle, String buyerName, BigDecimal amount,
                                String currency, Instant createdAt) {
}
