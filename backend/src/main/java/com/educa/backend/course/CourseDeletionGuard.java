package com.educa.backend.course;

/**
 * Point d'extension consulté avant la suppression d'un cours. Implémenté par les modules qui
 * dépendent de {@code course} (ex. {@code enrollment}) — inversion de dépendance pour éviter un cycle.
 */
public interface CourseDeletionGuard {

    /** {@code true} si le cours porte une activité d'apprenants (inscriptions…) qui interdit sa suppression. */
    boolean blocksDeletion(Long courseId);
}
