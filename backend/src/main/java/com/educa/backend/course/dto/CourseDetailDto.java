package com.educa.backend.course.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.educa.backend.course.CourseCategory;
import com.educa.backend.course.CourseLevel;

/**
 * @param promoPrice      prix promotionnel configuré ({@code null} si aucun) — actif seulement si {@code promoActive}
 * @param promoActive     la promotion s'applique maintenant (prix barré + prix promo affichés, montant payé)
 * @param previewVideoUrl URL de la vidéo de démo ({@code null} si aucune), visible avant l'achat
 */
public record CourseDetailDto(
        Long id,
        String slug,
        String title,
        String description,
        String language,
        boolean published,
        String instructorName,
        int controlWeight,
        int examWeight,
        int passThreshold,
        boolean contentsVisible,
        List<ChapterDto> chapters,
        BigDecimal price,
        String coverImageUrl,
        long learnerCount,
        Double averageRating,
        long ratingCount,
        String subtitle,
        CourseCategory category,
        CourseLevel level,
        BigDecimal durationHours,
        List<String> objectives,
        List<String> prerequisites,
        String targetAudience,
        String instructorHeadline,
        String instructorBio,
        BigDecimal promoPrice,
        Instant promoEndsAt,
        boolean promoActive,
        String previewVideoUrl) {
}
