package com.educa.backend.rating;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.web.PageResponse;
import com.educa.backend.rating.dto.AdminReviewDto;

/** Modération des avis : registre de toutes les notes et suppression (administration). */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final RatingService ratingService;

    public AdminReviewController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public PageResponse<AdminReviewDto> registry(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(ratingService.registry(PageRequest.of(Math.max(page, 0), safeSize)));
    }

    /** Supprime l'avis entier (note + texte) : la moyenne du cours est recalculée sans lui. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ratingService.delete(id);
    }
}
