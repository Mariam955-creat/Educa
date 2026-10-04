package com.educa.backend.course;

import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.course.dto.CourseSummaryDto;
import com.educa.backend.storage.FileTypeDetector;
import com.educa.backend.storage.StorageService;

/** Image de couverture d'un cours (carte du catalogue, en-tête de la page cours). */
@Service
public class CourseCoverService {

    /** Formats d'image acceptés — pas de SVG, qui peut embarquer du script. */
    private static final Set<String> IMAGE_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final StorageService storageService;
    private final FileTypeDetector fileTypeDetector;

    public CourseCoverService(CourseRepository courseRepository, CourseService courseService,
                              StorageService storageService, FileTypeDetector fileTypeDetector) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.storageService = storageService;
        this.fileTypeDetector = fileTypeDetector;
    }

    @Transactional
    public CourseSummaryDto upload(Long courseId, MultipartFile file) {
        Course course = courseService.requireOwned(courseId);
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        if (file.getSize() > MAX_COVER_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Image trop volumineuse (max 5 Mo)");
        }
        // Type réel détecté sur le contenu (magic bytes), jamais le Content-Type déclaré par le client
        String detected = fileTypeDetector.detect(file);
        if (!IMAGE_MIME_TYPES.contains(detected)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Image non autorisée (PNG, JPEG, WEBP ou GIF) : " + detected);
        }
        String previousKey = course.getCoverImageKey();
        course.setCoverImageKey(storageService.store(file, "covers/" + courseId));
        course.setCoverImageType(detected);
        courseRepository.saveAndFlush(course);
        if (previousKey != null) {
            storageService.delete(previousKey);
        }
        return courseService.summary(courseId);
    }

    @Transactional
    public CourseSummaryDto remove(Long courseId) {
        Course course = courseService.requireOwned(courseId);
        if (course.getCoverImageKey() != null) {
            storageService.delete(course.getCoverImageKey());
            course.setCoverImageKey(null);
            course.setCoverImageType(null);
            courseRepository.saveAndFlush(course);
        }
        return courseService.summary(courseId);
    }

    /** Couverture d'un cours publié (ou visible par son propriétaire/ADMIN) — 404 sinon. */
    @Transactional(readOnly = true)
    public Cover load(Long courseId) {
        Course course = courseService.requireCourse(courseId);
        if (!course.isPublished() && !courseService.isOwnerOrAdmin(courseId)) {
            throw new ResourceNotFoundException("Cours introuvable");
        }
        if (course.getCoverImageKey() == null) {
            throw new ResourceNotFoundException("Ce cours n'a pas d'image de couverture");
        }
        return new Cover(storageService.loadAsResource(course.getCoverImageKey()), course.getCoverImageType());
    }

    public record Cover(Resource resource, String mimeType) {
    }
}
