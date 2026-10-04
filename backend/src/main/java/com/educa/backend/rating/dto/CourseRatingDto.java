package com.educa.backend.rating.dto;

/**
 * @param average   note moyenne sur 5 (arrondie au dixième), {@code null} tant que personne n'a noté
 * @param myStars   note de l'utilisateur courant, {@code null} s'il n'a pas noté (ou s'il est anonyme)
 * @param myComment avis écrit de l'utilisateur courant, {@code null} s'il n'en a pas laissé
 * @param canRate   {@code true} si l'utilisateur courant est inscrit au cours
 */
public record CourseRatingDto(Double average, long count, Integer myStars, String myComment, boolean canRate) {
}
