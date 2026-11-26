package com.educa.backend.user;

import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.web.PageResponse;
import com.educa.backend.user.InstructorRequest.Status;
import com.educa.backend.user.dto.InstructorRequestDto;
import com.educa.backend.user.dto.RejectInstructorRequest;

import jakarta.validation.Valid;

/** Administration des demandes de rôle formateur : liste par état, nombre en attente, validation ou refus motivé. */
@RestController
@RequestMapping("/api/v1/admin/instructor-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInstructorRequestController {

    private final InstructorRequestService service;

    public AdminInstructorRequestController(InstructorRequestService service) {
        this.service = service;
    }

    /** Demandes d'un état (en attente par défaut) ; les plus anciennes d'abord en attente, les plus récentes sinon. */
    @GetMapping
    public PageResponse<InstructorRequestDto> list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Status parsed = parseStatus(status);
        Sort sort = parsed == Status.PENDING
                ? Sort.by("createdAt").ascending()
                : Sort.by("decidedAt").descending();
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(service.list(parsed, PageRequest.of(Math.max(page, 0), safeSize, sort)));
    }

    /** Nombre de demandes en attente (pastille du menu admin). */
    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of("pending", service.countPending());
    }

    @PostMapping("/{id}/approve")
    public InstructorRequestDto approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public InstructorRequestDto reject(@PathVariable Long id, @Valid @RequestBody RejectInstructorRequest request) {
        return service.reject(id, request.reason());
    }

    private static Status parseStatus(String value) {
        try {
            return Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "État de demande inconnu : " + value);
        }
    }
}
