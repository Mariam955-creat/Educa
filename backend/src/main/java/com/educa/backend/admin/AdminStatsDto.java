package com.educa.backend.admin;

import java.math.BigDecimal;

/** Indicateurs du tableau de bord admin ; « recent » = 30 derniers jours. */
public record AdminStatsDto(Users users, Courses courses, Enrollments enrollments, Revenue revenue,
                            long certificates, Reviews reviews) {

    public record Users(long total, long learners, long instructors, long admins, long disabled, long recent) {
    }

    public record Courses(long total, long published, long drafts) {
    }

    public record Enrollments(long total, long recent) {
    }

    public record Revenue(BigDecimal total, long sales, BigDecimal recent, long recentSales, String currency) {
    }

    /** @param average note moyenne sur 5, {@code null} sans avis */
    public record Reviews(long count, Double average) {
    }
}
