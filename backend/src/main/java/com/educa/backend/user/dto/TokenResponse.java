package com.educa.backend.user.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserDto user) {
}
