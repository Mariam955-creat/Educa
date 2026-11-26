package com.educa.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.security.CurrentUser;
import com.educa.backend.user.dto.InstructorRequestDto;

/** Demande de rôle formateur de l'utilisateur connecté (à l'inscription ou depuis l'espace compte). */
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

    /** Dépose la demande : motivation + dernier diplôme, CV et pièce d'identité (multipart, tous obligatoires). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorRequestDto submit(
            @RequestParam(required = false) String motivation,
            @RequestPart(required = false) MultipartFile diploma,
            @RequestPart(required = false) MultipartFile cv,
            @RequestPart(required = false) MultipartFile idDocument) {
        return service.submit(CurrentUser.id(), motivation, diploma, cv, idDocument);
    }
}
