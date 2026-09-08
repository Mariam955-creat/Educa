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

    /** Id de l'utilisateur courant, ou {@code null} si la requête est anonyme. */
    public static Long optionalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthPrincipal principal ? principal.id() : null;
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
