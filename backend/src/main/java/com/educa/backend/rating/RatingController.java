package com.educa.backend.rating;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.web.PageResponse;
import com.educa.backend.rating.dto.CourseRatingDto;
import com.educa.backend.rating.dto.CourseReviewDto;
import com.educa.backend.rating.dto.RateCourseRequest;
import com.educa.backend.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
public class RatingController {

    private static final int MAX_PAGE_SIZE = 50;

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /** Public (comme le catalogue) ; {@code myStars}/{@code canRate} sont renseignés si l'appelant est connecté. */
    @GetMapping("/rating")
    public CourseRatingDto get(@PathVariable Long courseId) {
        return ratingService.get(courseId, CurrentUser.optionalId());
    }

    @PutMapping("/rating")
    public CourseRatingDto rate(@PathVariable Long courseId, @Valid @RequestBody RateCourseRequest request) {
        return ratingService.rate(courseId, CurrentUser.id(), request.stars(), request.comment());
    }

    /** Public : avis écrits affichés sur la page du cours. */
    @GetMapping("/reviews")
    public PageResponse<CourseReviewDto> reviews(@PathVariable Long courseId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageResponse.of(ratingService.reviews(courseId, PageRequest.of(Math.max(page, 0), safeSize)));
    }
}
