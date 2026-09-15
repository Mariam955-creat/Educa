package com.educa.backend.course;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.course.dto.ChapterTranslationInput;
import com.educa.backend.course.dto.ChapterTranslationItem;
import com.educa.backend.course.dto.CourseTranslationEditDto;
import com.educa.backend.course.dto.UpdateCourseTranslationRequest;
import com.educa.backend.language.LanguageService;

/** Gestion des traductions (titre/description de cours, titre des chapitres) par le propriétaire du cours / un ADMIN. */
@Service
public class CourseTranslationService {

    private final CourseService courseService;
    private final ChapterRepository chapterRepository;
    private final CourseTranslationRepository courseTranslationRepository;
    private final ChapterTranslationRepository chapterTranslationRepository;
    private final LanguageService languageService;

    public CourseTranslationService(CourseService courseService, ChapterRepository chapterRepository,
                                    CourseTranslationRepository courseTranslationRepository,
                                    ChapterTranslationRepository chapterTranslationRepository,
                                    LanguageService languageService) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.courseTranslationRepository = courseTranslationRepository;
        this.chapterTranslationRepository = chapterTranslationRepository;
        this.languageService = languageService;
    }

    /** Langues dans lesquelles ce cours a déjà (au moins) une traduction de titre. */
    @Transactional(readOnly = true)
    public List<String> listLanguages(Long courseId) {
        courseService.requireOwned(courseId);
        return courseTranslationRepository.findByCourseId(courseId).stream()
                .map(CourseTranslation::getLanguageCode)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseTranslationEditDto edit(Long courseId, String languageCode) {
        courseService.requireOwned(courseId);
        CourseTranslation courseTranslation = courseTranslationRepository
                .findByCourseIdAndLanguageCode(courseId, languageCode).orElse(null);

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByPosition(courseId);
        Map<Long, String> translatedTitles = chapterTranslatedTitles(chapters, languageCode);
        List<ChapterTranslationItem> items = chapters.stream()
                .map(ch -> new ChapterTranslationItem(ch.getId(), ch.getTitle(), translatedTitles.get(ch.getId())))
                .toList();

        return new CourseTranslationEditDto(languageCode,
                courseTranslation != null ? courseTranslation.getTitle() : null,
                courseTranslation != null ? courseTranslation.getDescription() : null,
                items);
    }

    @Transactional
    public CourseTranslationEditDto save(Long courseId, String languageCode, UpdateCourseTranslationRequest request) {
        courseService.requireOwned(courseId);
        if (!languageService.isActive(languageCode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Langue indisponible : " + languageCode);
        }

        CourseTranslation translation = courseTranslationRepository
                .findByCourseIdAndLanguageCode(courseId, languageCode)
                .orElseGet(() -> {
                    CourseTranslation t = new CourseTranslation();
                    t.setCourseId(courseId);
                    t.setLanguageCode(languageCode);
                    return t;
                });
        translation.setTitle(request.courseTitle().trim());
        translation.setDescription(request.courseDescription());
        courseTranslationRepository.save(translation);

        if (request.chapters() != null && !request.chapters().isEmpty()) {
            Set<Long> ownChapterIds = chapterRepository.findByCourseIdOrderByPosition(courseId).stream()
                    .map(Chapter::getId).collect(Collectors.toSet());
            for (ChapterTranslationInput item : request.chapters()) {
                if (!ownChapterIds.contains(item.chapterId())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Le chapitre " + item.chapterId() + " n'appartient pas à ce cours");
                }
                saveChapterTranslation(item.chapterId(), languageCode, item.title());
            }
        }
        return edit(courseId, languageCode);
    }

    @Transactional
    public void delete(Long courseId, String languageCode) {
        courseService.requireOwned(courseId);
        courseTranslationRepository.deleteByCourseIdAndLanguageCode(courseId, languageCode);
        List<Long> chapterIds = chapterRepository.findByCourseIdOrderByPosition(courseId).stream()
                .map(Chapter::getId).toList();
        if (!chapterIds.isEmpty()) {
            chapterTranslationRepository.deleteByChapterIdInAndLanguageCode(chapterIds, languageCode);
        }
    }

    private void saveChapterTranslation(Long chapterId, String languageCode, String title) {
        if (!StringUtils.hasText(title)) {
            chapterTranslationRepository.deleteByChapterIdAndLanguageCode(chapterId, languageCode);
            return;
        }
        ChapterTranslation translation = chapterTranslationRepository
                .findByChapterIdAndLanguageCode(chapterId, languageCode)
                .orElseGet(() -> {
                    ChapterTranslation t = new ChapterTranslation();
                    t.setChapterId(chapterId);
                    t.setLanguageCode(languageCode);
                    return t;
                });
        translation.setTitle(title.trim());
        chapterTranslationRepository.save(translation);
    }

    private Map<Long, String> chapterTranslatedTitles(List<Chapter> chapters, String languageCode) {
        if (chapters.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = chapters.stream().map(Chapter::getId).toList();
        return chapterTranslationRepository.findByChapterIdInAndLanguageCode(ids, languageCode).stream()
                .collect(Collectors.toMap(ChapterTranslation::getChapterId, ChapterTranslation::getTitle));
    }
}
