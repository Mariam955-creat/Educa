package com.educa.backend.language;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.language.dto.LanguageDto;
import com.educa.backend.language.dto.UpdateLanguageActiveRequest;

import jakarta.validation.Valid;

/** Gestion des langues actives (administration). */
@RestController
@RequestMapping("/api/v1/admin/languages")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLanguageController {

    private final LanguageService languageService;

    public AdminLanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public List<LanguageDto> list() {
        return languageService.listAll();
    }

    @PatchMapping("/{code}")
    public LanguageDto setActive(@PathVariable String code, @Valid @RequestBody UpdateLanguageActiveRequest request) {
        return languageService.setActive(code, request.active());
    }
}
