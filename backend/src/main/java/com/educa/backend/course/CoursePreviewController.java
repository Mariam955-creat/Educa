package com.educa.backend.course;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.course.dto.CourseSummaryDto;

@RestController
@RequestMapping("/api/v1/courses/{id}/preview")
public class CoursePreviewController {

    private final CoursePreviewService previewService;

    public CoursePreviewController(CoursePreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return previewService.upload(id, file);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto remove(@PathVariable Long id) {
        return previewService.remove(id);
    }

    /**
     * Public (comme la couverture) : une balise {@code <video>} n'envoie pas le jeton JWT. Les requêtes
     * {@code Range} (lecture/avance dans la vidéo) sont gérées par Spring pour un corps {@link Resource}.
     */
    @GetMapping
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        CoursePreviewService.Preview preview = previewService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(preview.mimeType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(preview.resource());
    }
}
