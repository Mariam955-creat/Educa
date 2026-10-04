package com.educa.backend.course;

import java.util.Collection;
import java.util.Map;

/**
 * Nombre d'apprenants par cours, affiché sur les cartes du catalogue. Implémenté par {@code enrollment}
 * — inversion de dépendance (comme {@link CourseDeletionGuard}) pour éviter un cycle.
 */
public interface CourseAudienceProvider {

    /** Nombre d'inscrits par id de cours ; un cours absent de la map n'a aucun inscrit. */
    Map<Long, Long> learnerCounts(Collection<Long> courseIds);
}
