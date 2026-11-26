package com.educa.backend.course;

/** Étape de validation d'un cours par l'administration (indépendante de sa visibilité, {@code published}). */
public enum CourseReviewStatus {
    /** Jamais soumis (ou retiré avant décision). */
    DRAFT,
    /** Soumis par le formateur, en attente de décision. */
    PENDING,
    /** Validé : le formateur peut le publier ou le retirer librement. */
    APPROVED,
    /** Refusé avec un motif : le formateur corrige puis resoumet. */
    REJECTED
}
