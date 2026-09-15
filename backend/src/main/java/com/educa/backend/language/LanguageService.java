package com.educa.backend.language;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ConflictException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.language.dto.LanguageDto;

@Service
public class LanguageService {

    private final LanguageRepository languageRepository;

    public LanguageService(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Transactional(readOnly = true)
    public List<LanguageDto> listActive() {
        return languageRepository.findAllByActiveTrueOrderByCodeAsc().stream().map(LanguageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LanguageDto> listAll() {
        return languageRepository.findAllByOrderByCodeAsc().stream().map(LanguageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public boolean isActive(String code) {
        return code != null && languageRepository.findByCodeAndActiveTrue(code).isPresent();
    }

    @Transactional
    public LanguageDto setActive(String code, boolean active) {
        Language language = languageRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Langue inconnue : " + code));
        if (!active && language.isActive() && languageRepository.countByActiveTrue() <= 1) {
            throw new ConflictException("Impossible de désactiver la dernière langue active");
        }
        language.setActive(active);
        return toDto(languageRepository.save(language));
    }

    private static LanguageDto toDto(Language language) {
        return new LanguageDto(language.getCode(), language.getName(), language.isActive());
    }
}
