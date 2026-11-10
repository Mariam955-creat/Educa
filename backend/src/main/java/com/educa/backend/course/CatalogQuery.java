package com.educa.backend.course;

import java.math.BigDecimal;

/**
 * Critères de recherche du catalogue public. Tous facultatifs ({@code null} = pas de filtre).
 *
 * @param q          texte cherché dans le titre et le sous-titre
 * @param language   langue du contenu du cours (code ISO, ex. {@code fr})
 * @param maxPrice   prix maximal, comparé au prix réellement payé (promotion comprise)
 * @param minRating  note moyenne minimale (sur 5) ; un cours sans note est exclu
 * @param promoOnly  seulement les cours dont la promotion est en cours
 * @param sort       ordre des résultats ({@link CatalogSort#NEWEST} si {@code null})
 */
public record CatalogQuery(
        String q,
        String language,
        CourseCategory category,
        CourseLevel level,
        BigDecimal maxPrice,
        Double minRating,
        boolean promoOnly,
        CatalogSort sort) {

    /** Ordre des résultats du catalogue. */
    public enum CatalogSort {
        /** Les plus récents d'abord. */
        NEWEST,
        /** Les plus suivis (nombre d'inscrits) d'abord. */
        POPULAR,
        /** Les mieux notés d'abord ; les cours sans note en dernier. */
        RATING,
        /** Prix payé croissant. */
        PRICE_ASC,
        /** Prix payé décroissant. */
        PRICE_DESC
    }
}
