package com.educa.backend.rating;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Note d'un apprenant pour un cours : une seule par (cours, apprenant), modifiable. Les avis mis à la corbeille
 * par la modération ({@code deleted_at} renseigné) sont exclus de toutes les requêtes JPA — moyennes comprises ;
 * seule la corbeille les lit, en SQL natif ({@link CourseRatingRepository}).
 */
@Entity
@SQLRestriction("deleted_at is null")
@Table(name = "course_ratings")
@Getter
@Setter
@NoArgsConstructor
public class CourseRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Couplage par id avec les modules {@code course} et {@code user}, pas de relation JPA. */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private short stars;

    /** Avis écrit facultatif. */
    @Column(columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
