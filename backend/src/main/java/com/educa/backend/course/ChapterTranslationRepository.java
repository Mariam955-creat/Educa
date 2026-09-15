package com.educa.backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterTranslationRepository extends JpaRepository<ChapterTranslation, Long> {

    Optional<ChapterTranslation> findByChapterIdAndLanguageCode(Long chapterId, String languageCode);

    List<ChapterTranslation> findByChapterIdInAndLanguageCode(List<Long> chapterIds, String languageCode);

    void deleteByChapterIdAndLanguageCode(Long chapterId, String languageCode);

    void deleteByChapterIdInAndLanguageCode(List<Long> chapterIds, String languageCode);
}
