package com.educa.backend.user.dto;

import java.time.Instant;
import java.util.Set;

/** Compte à la corbeille (administration). */
public record TrashedUserDto(Long id, String email, String fullName, Set<String> roles, Instant deletedAt) {
}
