package com.educa.backend.rating;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.rating.dto.AdminReviewDto;
import com.educa.backend.security.CurrentUser;

/** Avis reçus par les cours du formateur connecté (espace formateur). Lecture seule : la suppression reste à l'admin. */
@RestController
@RequestMapping("/api/v1/instructor/reviews")
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public class InstructorReviewController {

    private final RatingService ratingService;

    public InstructorReviewController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public List<AdminReviewDto> received() {
        return ratingService.receivedByInstructor(CurrentUser.id());
    }
}
