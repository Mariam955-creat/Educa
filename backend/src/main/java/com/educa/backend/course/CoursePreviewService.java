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

/**
 * Vidéo de démo d'un cours (environ une minute), visible sur la page du cours avant l'achat.
 * Même patron que {@link CourseCoverService} : type réel détecté sur le contenu, fichier précédent supprimé.
 */
@Service
public class CoursePreviewService {

    private static final Set<String> VIDEO_MIME_TYPES = Set.of("video/mp4", "video/webm");

    /** Une minute de vidéo en bonne qualité tient largement sous cette taille. */
    private static final long MAX_PREVIEW_BYTES = 50L * 1024 * 1024;

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final StorageService storageService;
    private final FileTypeDetector fileTypeDetector;

    public CoursePreviewService(CourseRepository courseRepository, CourseService courseService,
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
        if (file.getSize() > MAX_PREVIEW_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Vidéo trop volumineuse (max 50 Mo, environ une minute)");
        }
        String detected = fileTypeDetector.detect(file);
        if (!VIDEO_MIME_TYPES.contains(detected)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Vidéo non autorisée (MP4 ou WEBM) : " + detected);
        }
        String previousKey = course.getPreviewVideoKey();
        course.setPreviewVideoKey(storageService.store(file, "previews/" + courseId));
        course.setPreviewVideoType(detected);
        courseRepository.saveAndFlush(course);
        if (previousKey != null) {
            storageService.delete(previousKey);
        }
        return courseService.summary(courseId);
    }

    @Transactional
    public CourseSummaryDto remove(Long courseId) {
        Course course = courseService.requireOwned(courseId);
        if (course.getPreviewVideoKey() != null) {
            storageService.delete(course.getPreviewVideoKey());
            course.setPreviewVideoKey(null);
            course.setPreviewVideoType(null);
            courseRepository.saveAndFlush(course);
        }
        return courseService.summary(courseId);
    }

    /** Vidéo de démo d'un cours publié (ou visible par son propriétaire/ADMIN) — 404 sinon. */
    @Transactional(readOnly = true)
    public Preview load(Long courseId) {
        Course course = courseService.requireCourse(courseId);
        if (!course.isPublished() && !courseService.isOwnerOrAdmin(courseId)) {
            throw new ResourceNotFoundException("Cours introuvable");
        }
        if (course.getPreviewVideoKey() == null) {
            throw new ResourceNotFoundException("Ce cours n'a pas de vidéo de démo");
        }
        return new Preview(storageService.loadAsResource(course.getPreviewVideoKey()), course.getPreviewVideoType());
    }

    public record Preview(Resource resource, String mimeType) {
    }
}
