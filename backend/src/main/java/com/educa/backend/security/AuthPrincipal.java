package com.educa.backend.security;

/**
 * Identité de l'utilisateur authentifié, portée par le {@code Authentication} (principal).
 * Alimentée à partir des claims du JWT — aucun accès base par requête.
 */
public record AuthPrincipal(Long id, String email) {
}
