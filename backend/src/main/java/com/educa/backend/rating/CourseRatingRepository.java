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

    // ---------- corbeille (SQL natif : l'entité masque les avis supprimés) ----------

    /** Ligne d'un avis à la corbeille. */
    interface TrashedRatingRow {
        Long getId();

        Long getCourseId();

        Long getUserId();

        Integer getStars();

        String getComment();

        java.time.Instant getDeletedAt();
    }

    @Query(nativeQuery = true, value = """
            select id, course_id as courseId, user_id as userId, stars, comment, deleted_at as deletedAt
            from course_ratings where deleted_at is not null order by deleted_at desc
            """)
    List<TrashedRatingRow> findTrash();

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = "update course_ratings set deleted_at = now() where id = :id and deleted_at is null")
    int moveToTrash(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @Query(nativeQuery = true, value = "update course_ratings set deleted_at = null where id = :id and deleted_at is not null")
    int restoreFromTrash(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @Query(nativeQuery = true, value = "delete from course_ratings where id = :id and deleted_at is not null")
    int deleteFromTrash(@Param("id") Long id);

    /** Purge l'ancien avis à la corbeille d'un apprenant qui note à nouveau (unicité cours/apprenant). */
    @org.springframework.data.jpa.repository.Modifying
    @Query(nativeQuery = true, value = """
            delete from course_ratings where course_id = :courseId and user_id = :userId and deleted_at is not null
            """)
    int purgeTrashed(@Param("courseId") Long courseId, @Param("userId") Long userId);

    Optional<CourseRating> findByCourseIdAndUserId(Long courseId, Long userId);

    /** Avis écrits d'un cours, du plus récent au plus ancien (les notes sans commentaire n'y figurent pas). */
    Page<CourseRating> findByCourseIdAndCommentIsNotNullOrderByUpdatedAtDesc(Long courseId, Pageable pageable);

    /** Notes d'un utilisateur, de la plus récente à la plus ancienne (son espace « Mon compte »). */
    List<CourseRating> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /** Notes reçues par un ensemble de cours (ceux d'un formateur), de la plus récente à la plus ancienne. */
    List<CourseRating> findByCourseIdInOrderByUpdatedAtDesc(Collection<Long> courseIds);

    /** Ligne {@code [moyenne, nombre]} de toutes les notes de la plateforme. */
    @Query("select avg(r.stars), count(r) from CourseRating r")
    List<Object[]> globalStats();

    /** Toutes les notes, de la plus récente à la plus ancienne (modération). */
    Page<CourseRating> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    /** Lignes {@code [courseId, moyenne, nombre de notes]} pour les cours donnés. */
    @Query("""
            select r.courseId, avg(r.stars), count(r) from CourseRating r
            where r.courseId in :courseIds group by r.courseId
            """)
    List<Object[]> statsByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}
