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

    @Query("""
            select c from Course c
            where c.published = true
              and (cast(:q as string) is null or lower(c.title) like lower(concat('%', cast(:q as string), '%')))
              and (cast(:language as string) is null or c.language = cast(:language as string))
            order by c.createdAt desc
            """)
    Page<Course> searchPublished(@Param("q") String q, @Param("language") String language, Pageable pageable);

    long countByInstructorId(Long instructorId);
}
