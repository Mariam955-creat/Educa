package com.educa.backend.language;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, String> {

    List<Language> findAllByOrderByCodeAsc();

    List<Language> findAllByActiveTrueOrderByCodeAsc();

    long countByActiveTrue();

    Optional<Language> findByCodeAndActiveTrue(String code);
}
