package com.educa.backend.user;

/**
 * Rôles applicatifs (RBAC). Un utilisateur a un seul rôle actif au MVP
 * (LEARNER par défaut ; promotion INSTRUCTOR par un ADMIN).
 */
public enum RoleName {
    LEARNER,
    INSTRUCTOR,
    ADMIN
}
