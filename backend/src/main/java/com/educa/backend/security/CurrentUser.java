package com.educa.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.educa.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Accès à l'utilisateur courant depuis le contexte de sécurité.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthPrincipal require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentification requise");
    }

    public static Long id() {
        return require().id();
    }
}
