package com.educa.backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTranslationRepository extends JpaRepository<CourseTranslation, Long> {

    Optional<CourseTranslation> findByCourseIdAndLanguageCode(Long courseId, String languageCode);

    List<CourseTranslation> findByCourseId(Long courseId);

    List<CourseTranslation> findByCourseIdInAndLanguageCode(List<Long> courseIds, String languageCode);

    void deleteByCourseIdAndLanguageCode(Long courseId, String languageCode);
}
