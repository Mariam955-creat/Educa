package com.educa.backend.rating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRatingRepository extends JpaRepository<CourseRating, Long> {

    Optional<CourseRating> findByCourseIdAndUserId(Long courseId, Long userId);

    /** Avis écrits d'un cours, du plus récent au plus ancien (les notes sans commentaire n'y figurent pas). */
    Page<CourseRating> findByCourseIdAndCommentIsNotNullOrderByUpdatedAtDesc(Long courseId, Pageable pageable);

    /** Notes d'un utilisateur, de la plus récente à la plus ancienne (son espace « Mon compte »). */
    List<CourseRating> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /** Toutes les notes, de la plus récente à la plus ancienne (modération). */
    Page<CourseRating> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    /** Lignes {@code [courseId, moyenne, nombre de notes]} pour les cours donnés. */
    @Query("""
            select r.courseId, avg(r.stars), count(r) from CourseRating r
            where r.courseId in :courseIds group by r.courseId
            """)
    List<Object[]> statsByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
