package com.educa.backend.rating;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.rating.dto.MyReviewDto;
import com.educa.backend.security.CurrentUser;

/** Avis de l'utilisateur connecté (espace « Mon compte »). */
@RestController
@RequestMapping("/api/v1/me/reviews")
public class MyReviewController {

    private final RatingService ratingService;

    public MyReviewController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public List<MyReviewDto> mine() {
        return ratingService.mine(CurrentUser.id());
    }

    /** Retire son propre avis (note + texte) ; {@code 404} s'il n'existe pas ou appartient à un autre utilisateur. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ratingService.deleteMine(id, CurrentUser.id());
    }
}
