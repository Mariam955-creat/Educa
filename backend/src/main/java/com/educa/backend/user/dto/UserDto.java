package com.educa.backend.user.dto;

import java.util.Set;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String preferredLanguage,
        Set<String> roles) {
}
