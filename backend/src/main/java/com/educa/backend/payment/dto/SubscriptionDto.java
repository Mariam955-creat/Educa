package com.educa.backend.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Offre d'abonnement et, pour un utilisateur connecté, son état.
 *
 * @param price       prix d'un pass (par mois)
 * @param days        durée d'accès accordée par un pass
 * @param active      l'utilisateur a un accès en cours ({@code false} pour un visiteur)
 * @param accessUntil fin de l'accès en cours ({@code null} sans abonnement actif)
 */
public record SubscriptionDto(BigDecimal price, String currency, int days, boolean active, Instant accessUntil) {
}
