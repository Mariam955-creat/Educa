package com.educa.backend.course;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.course.dto.ContentDto;
import com.educa.backend.course.dto.ContentRequest;
import com.educa.backend.enrollment.EnrollmentService;
import com.educa.backend.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ContentController {

    private final ContentService contentService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public ContentController(ContentService contentService, CourseService courseService,
                             EnrollmentService enrollmentService) {
        this.contentService = contentService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/chapters/{chapterId}/contents")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentDto create(@PathVariable Long chapterId, @Valid @RequestBody ContentRequest request) {
        return contentService.create(chapterId, request);
    }

    @PutMapping("/contents/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ContentDto update(@PathVariable Long id, @Valid @RequestBody ContentRequest request) {
        return contentService.update(id, request);
    }

    @DeleteMapping("/contents/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contentService.delete(id);
    }

    @PostMapping("/contents/{id}/file")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ContentDto uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return contentService.attachFile(id, file);
    }

    @GetMapping("/contents/{id}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Content content = contentService.requireForReading(id);
        Long courseId = contentService.courseIdOf(id);

        boolean privileged = courseService.isOwnerOrAdmin(courseId);
        boolean enrolled = enrollmentService.isEnrolled(CurrentUser.id(), courseId);
        if (!privileged && !enrolled) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Inscrivez-vous au cours pour accéder à ce contenu");
        }
        if (!content.hasFile()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Aucun fichier associé à ce contenu");
        }

        Resource resource = contentService.loadFile(content);

        // Le type MIME est fourni par le téléverseur : on le parse défensivement et on ne
        // sert « inline » que les formats sûrs (les autres en pièce jointe) pour éviter
        // qu'un HTML/SVG piégé s'exécute sur l'origine de l'API.
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (content.getMimeType() != null) {
            try {
                mediaType = MediaType.parseMediaType(content.getMimeType());
            } catch (InvalidMediaTypeException ignored) {
                // reste application/octet-stream
            }
        }
        String disposition = (isInlineSafe(mediaType) ? "inline" : "attachment")
                + "; filename=\"" + sanitizeFilename(content.getFileName()) + "\"";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private static final List<MediaType> INLINE_SAFE = List.of(
            MediaType.APPLICATION_PDF,
            new MediaType("image"),
            new MediaType("video"),
            new MediaType("audio"));

    private static boolean isInlineSafe(MediaType type) {
        if (MediaType.valueOf("image/svg+xml").isCompatibleWith(type)) {
            return false; // un SVG peut embarquer du script
        }
        return INLINE_SAFE.stream().anyMatch(safe -> safe.isCompatibleWith(type));
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "fichier";
        }
        String cleaned = name.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        return cleaned.isBlank() ? "fichier" : cleaned;
    }
}
