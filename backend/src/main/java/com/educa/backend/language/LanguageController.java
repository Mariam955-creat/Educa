package com.educa.backend.language;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.language.dto.LanguageDto;

/** Langues actives disponibles pour le contenu des cours (voir SecurityConfig : permitAll). */
@RestController
@RequestMapping("/api/v1/languages")
public class LanguageController {

    private final LanguageService languageService;

    public LanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public List<LanguageDto> active() {
        return languageService.listActive();
    }
}
