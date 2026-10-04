package com.educa.backend.user;

/**
 * Point d'extension consulté avant la suppression définitive d'un compte. Implémenté par les modules qui
 * dépendent de {@code user} (ex. {@code course}) — inversion de dépendance pour éviter un cycle.
 */
public interface UserDeletionGuard {

    /** Message d'erreur si le compte ne peut pas être supprimé définitivement, {@code null} sinon. */
    String blockingReason(Long userId);
}
