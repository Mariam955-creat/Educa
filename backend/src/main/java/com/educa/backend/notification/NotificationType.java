package com.educa.backend.notification;

/** Type d'une notification : l'interface en tire le texte traduit (le serveur ne stocke pas de phrase). */
public enum NotificationType {
    /** Pour les administrateurs : une nouvelle demande de rôle formateur attend une décision. */
    INSTRUCTOR_REQUEST_SUBMITTED,
    INSTRUCTOR_REQUEST_APPROVED,
    INSTRUCTOR_REQUEST_REJECTED
}
