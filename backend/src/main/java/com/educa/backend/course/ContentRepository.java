package com.educa.backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findByChapterIdOrderByPosition(Long chapterId);

    boolean existsByChapterIdAndPosition(Long chapterId, int position);

    long countByChapter_Course_Id(Long courseId);

    @Query("select c.chapter.course.id from Content c where c.id = :contentId")
    Optional<Long> findCourseId(Long contentId);
}
