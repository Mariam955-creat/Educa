package com.educa.backend.user.dto;

import java.time.Instant;
import java.util.Set;

public record AdminUserDto(
        Long id,
        String email,
        String fullName,
        Set<String> roles,
        boolean enabled,
        Instant createdAt) {
}
