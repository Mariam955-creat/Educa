package com.educa.backend.course;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.dto.ChapterDto;
import com.educa.backend.course.dto.ChapterRequest;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseService courseService;
    private final CourseMapper mapper;

    public ChapterService(ChapterRepository chapterRepository, CourseService courseService, CourseMapper mapper) {
        this.chapterRepository = chapterRepository;
        this.courseService = courseService;
        this.mapper = mapper;
    }

    @Transactional
    public ChapterDto create(Long courseId, ChapterRequest request) {
        Course course = courseService.requireOwned(courseId);
        if (chapterRepository.existsByCourseIdAndPosition(courseId, request.position())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un chapitre occupe déjà la position " + request.position());
        }
        Chapter chapter = new Chapter();
        chapter.setTitle(request.title().trim());
        chapter.setPosition(request.position());
        course.addChapter(chapter);
        return mapper.toChapterDto(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterDto update(Long chapterId, ChapterRequest request) {
        Chapter chapter = requireOwned(chapterId);
        if (request.position() != chapter.getPosition()
                && chapterRepository.existsByCourseIdAndPosition(chapter.getCourse().getId(), request.position())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un chapitre occupe déjà la position " + request.position());
        }
        chapter.setTitle(request.title().trim());
        chapter.setPosition(request.position());
        return mapper.toChapterDto(chapter);
    }

    @Transactional
    public void delete(Long chapterId) {
        chapterRepository.delete(requireOwned(chapterId));
    }

    @Transactional(readOnly = true)
    public Chapter requireOwned(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre introuvable"));
        courseService.requireOwned(chapter.getCourse().getId());
        return chapter;
    }
}
