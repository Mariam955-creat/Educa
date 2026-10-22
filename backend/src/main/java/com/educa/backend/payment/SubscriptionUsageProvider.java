package com.educa.backend.payment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Cours suivis grâce à l'abonnement, pour répartir le prix des pass entre les formateurs. Implémenté par
 * {@code enrollment} — inversion de dépendance (comme {@code CourseAudienceProvider}) pour éviter un cycle.
 */
public interface SubscriptionUsageProvider {

    /** Inscriptions par abonnement des utilisateurs donnés. */
    List<SubscriptionUse> subscriptionUses(Collection<Long> userIds);

    record SubscriptionUse(Long userId, Long courseId, Instant enrolledAt) {
    }
}
