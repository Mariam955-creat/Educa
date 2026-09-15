package com.educa.backend.user.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record UpdateRolesRequest(@NotEmpty Set<String> roles) {
}
