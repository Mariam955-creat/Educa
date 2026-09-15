package com.educa.backend.language.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateLanguageActiveRequest(@NotNull Boolean active) {
}
