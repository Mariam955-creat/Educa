package com.educa.backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Course> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);

    List<Course> findByInstructorIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long instructorId);

    /** Corbeille d'un formateur ({@code instructorId} renseigné) ou de toute la plateforme ({@code null}). */
    @Query("""
            select c from Course c
            where c.deletedAt is not null
              and (cast(:instructorId as long) is null or c.instructorId = :instructorId)
            order by c.deletedAt desc
            """)
    List<Course> findTrash(@Param("instructorId") Long instructorId);

    long countByDeletedAtIsNull();

    @Query("""
            select c from Course c
            where c.published = true
              and (cast(:q as string) is null or lower(c.title) like lower(concat('%', cast(:q as string), '%')))
              and (cast(:language as string) is null or c.language = cast(:language as string))
            order by c.createdAt desc
            """)
    Page<Course> searchPublished(@Param("q") String q, @Param("language") String language, Pageable pageable);

    long countByInstructorId(Long instructorId);

    long countByPublishedTrue();

    /** Tous les cours (administration) : recherche sur le titre, filtre facultatif sur la publication. */
    @Query("""
            select c from Course c
            where c.deletedAt is null
              and (cast(:q as string) is null or lower(c.title) like lower(concat('%', cast(:q as string), '%')))
              and (:published is null or c.published = :published)
            order by c.createdAt desc
            """)
    Page<Course> searchAll(@Param("q") String q, @Param("published") Boolean published, Pageable pageable);
}
