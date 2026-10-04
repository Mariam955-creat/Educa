package com.educa.backend.rating.dto;

/**
 * @param average          note moyenne sur 5 (arrondie au dixième), {@code null} tant que personne n'a noté
 * @param myStars          note de l'utilisateur courant, {@code null} s'il n'a pas noté (ou s'il est anonyme)
 * @param myComment        avis écrit de l'utilisateur courant, {@code null} s'il n'en a pas laissé
 * @param enrolled         {@code true} si l'utilisateur courant est inscrit au cours
 * @param progressPercent  progression de l'utilisateur courant dans le cours (0 s'il n'est pas inscrit)
 * @param requiredProgress progression minimale pour noter (en %)
 * @param canRate          {@code true} si l'utilisateur est inscrit et a atteint {@code requiredProgress}
 */
public record CourseRatingDto(Double average, long count, Integer myStars, String myComment, boolean enrolled,
                              int progressPercent, int requiredProgress, boolean canRate) {
}
