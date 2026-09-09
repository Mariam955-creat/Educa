package com.educa.backend.course;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.config.EducaProperties;
import com.educa.backend.course.dto.ContentDto;
import com.educa.backend.course.dto.ContentRequest;
import com.educa.backend.storage.StorageService;

@Service
public class ContentService {

    /** Types MIME acceptés pour un contenu VIDEO. */
    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime");

    /** Types MIME acceptés pour un contenu DOCUMENT (documents bureautiques, PDF, images, archives). */
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv", "text/markdown",
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/zip");

    private final ContentRepository contentRepository;
    private final ChapterService chapterService;
    private final CourseMapper mapper;
    private final StorageService storageService;
    private final EducaProperties properties;

    public ContentService(ContentRepository contentRepository, ChapterService chapterService,
                          CourseMapper mapper, StorageService storageService, EducaProperties properties) {
        this.contentRepository = contentRepository;
        this.chapterService = chapterService;
        this.mapper = mapper;
        this.storageService = storageService;
        this.properties = properties;
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
        validateUpload(file, content.getType());
        if (content.hasFile()) {
            storageService.delete(content.getFileKey());
        }
        String key = storageService.store(file, "contents/" + contentId);
        content.setFileKey(key);
        content.setFileName(StringUtils.getFilename(file.getOriginalFilename()));
        content.setMimeType(file.getContentType().toLowerCase());
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

    /** Contrôle la taille et le type MIME du fichier téléversé avant écriture sur le stockage. */
    private void validateUpload(MultipartFile file, ContentType type) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        long maxBytes = (long) properties.storage().maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Fichier trop volumineux (max " + properties.storage().maxFileSizeMb() + " Mo)");
        }
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        Set<String> allowed = type == ContentType.VIDEO ? VIDEO_MIME_TYPES : DOCUMENT_MIME_TYPES;
        if (!allowed.contains(mime)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Type de fichier non autorisé : " + (mime.isBlank() ? "inconnu" : mime));
        }
    }

    private void apply(Content content, ContentRequest request) {
        content.setType(request.type());
        content.setTitle(request.title().trim());
        content.setPosition(request.position());
        content.setTextBody(request.type() == ContentType.TEXT ? request.textBody() : null);
    }
}
