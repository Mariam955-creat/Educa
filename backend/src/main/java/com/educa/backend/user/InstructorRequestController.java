package com.educa.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.security.CurrentUser;
import com.educa.backend.user.dto.CreateInstructorRequest;
import com.educa.backend.user.dto.InstructorRequestDto;

import jakarta.validation.Valid;

/** Demande de rôle formateur de l'utilisateur connecté (« Devenir formateur » dans l'espace compte). */
@RestController
@RequestMapping("/api/v1/me/instructor-request")
public class InstructorRequestController {

    private final InstructorRequestService service;

    public InstructorRequestController(InstructorRequestService service) {
        this.service = service;
    }

    /** Dernière demande (en attente, acceptée ou refusée avec son motif) ; 204 s'il n'y en a jamais eu. */
    @GetMapping
    public ResponseEntity<InstructorRequestDto> latest() {
        return service.latest(CurrentUser.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorRequestDto submit(@Valid @RequestBody(required = false) CreateInstructorRequest request) {
        return service.submit(CurrentUser.id(), request != null ? request.motivation() : null);
    }
}
