package com.educa.backend.course;

import java.util.Collection;
import java.util.Map;

/**
 * Note moyenne des cours, affichée sur les cartes du catalogue. Implémenté par {@code rating}
 * — inversion de dépendance (comme {@link CourseDeletionGuard}) pour éviter un cycle.
 */
public interface CourseRatingProvider {

    /** Statistiques de notes par id de cours ; un cours absent de la map n'a aucune note. */
    Map<Long, RatingStats> ratingStats(Collection<Long> courseIds);

    /** @param average moyenne des étoiles (1 à 5), arrondie au dixième */
    record RatingStats(double average, long count) {
    }
}
