package com.educa.backend.course;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.dto.ContentDto;
import com.educa.backend.course.dto.ContentRequest;
import com.educa.backend.storage.StorageService;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ChapterService chapterService;
    private final CourseMapper mapper;
    private final StorageService storageService;

    public ContentService(ContentRepository contentRepository, ChapterService chapterService,
                          CourseMapper mapper, StorageService storageService) {
        this.contentRepository = contentRepository;
        this.chapterService = chapterService;
        this.mapper = mapper;
        this.storageService = storageService;
    }

    @Transactional
    public ContentDto create(Long chapterId, ContentRequest request) {
        Chapter chapter = chapterService.requireOwned(chapterId);
        if (contentRepository.existsByChapterIdAndPosition(chapterId, request.position())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un contenu occupe déjà la position " + request.position());
        }
        Content content = new Content();
        apply(content, request);
        chapter.addContent(content);
        return mapper.toContentDto(contentRepository.save(content));
    }

    @Transactional
    public ContentDto update(Long contentId, ContentRequest request) {
        Content content = requireOwned(contentId);
        if (request.position() != content.getPosition()
                && contentRepository.existsByChapterIdAndPosition(content.getChapter().getId(), request.position())) {
            throw new ApiException(HttpStatus.CONFLICT, "Un contenu occupe déjà la position " + request.position());
        }
        apply(content, request);
        return mapper.toContentDto(content);
    }

    @Transactional
    public void delete(Long contentId) {
        Content content = requireOwned(contentId);
        if (content.hasFile()) {
            storageService.delete(content.getFileKey());
        }
        contentRepository.delete(content);
    }

    @Transactional
    public ContentDto attachFile(Long contentId, MultipartFile file) {
        Content content = requireOwned(contentId);
        if (content.getType() == ContentType.TEXT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un contenu TEXT ne porte pas de fichier");
        }
        if (content.hasFile()) {
            storageService.delete(content.getFileKey());
        }
        String key = storageService.store(file, "contents/" + contentId);
        content.setFileKey(key);
        content.setFileName(file.getOriginalFilename());
        content.setMimeType(file.getContentType());
        return mapper.toContentDto(content);
    }

    @Transactional(readOnly = true)
    public Content requireOwned(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable"));
        chapterService.requireOwned(content.getChapter().getId());
        return content;
    }

    @Transactional(readOnly = true)
    public Content requireForReading(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable"));
    }

    @Transactional(readOnly = true)
    public Long courseIdOf(Long contentId) {
        return contentRepository.findCourseId(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenu introuvable"));
    }

    public org.springframework.core.io.Resource loadFile(Content content) {
        return storageService.loadAsResource(content.getFileKey());
    }

    private void apply(Content content, ContentRequest request) {
        content.setType(request.type());
        content.setTitle(request.title().trim());
        content.setPosition(request.position());
        content.setTextBody(request.type() == ContentType.TEXT ? request.textBody() : null);
    }
}
