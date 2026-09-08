package com.educa.backend.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByCourseIdOrderByPosition(Long courseId);

    boolean existsByCourseIdAndPosition(Long courseId, int position);
}
