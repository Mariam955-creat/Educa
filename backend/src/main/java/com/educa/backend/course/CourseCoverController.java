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
@RequestMapping("/api/v1/courses/{id}/cover")
public class CourseCoverController {

    private final CourseCoverService coverService;

    public CourseCoverController(CourseCoverService coverService) {
        this.coverService = coverService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return coverService.upload(id, file);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public CourseSummaryDto remove(@PathVariable Long id) {
        return coverService.remove(id);
    }

    /**
     * Public (comme le catalogue) : une balise {@code <img>} n'envoie pas le jeton JWT. L'URL est versionnée
     * ({@code ?v=}) à chaque nouvelle image, d'où un cache navigateur long.
     */
    @GetMapping
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        CourseCoverService.Cover cover = coverService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.mimeType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(cover.resource());
    }
}
