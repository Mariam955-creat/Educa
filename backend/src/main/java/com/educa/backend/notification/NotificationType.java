package com.educa.backend.notification;

/** Type d'une notification : l'interface en tire le texte traduit (le serveur ne stocke pas de phrase). */
public enum NotificationType {
    INSTRUCTOR_REQUEST_APPROVED,
    INSTRUCTOR_REQUEST_REJECTED
}
