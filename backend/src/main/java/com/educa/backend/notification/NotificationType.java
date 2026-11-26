package com.educa.backend.notification;

/** Type d'une notification : l'interface en tire le texte traduit (le serveur ne stocke pas de phrase). */
public enum NotificationType {
    /** Pour les administrateurs : une nouvelle demande de rôle formateur attend une décision. */
    INSTRUCTOR_REQUEST_SUBMITTED,
    INSTRUCTOR_REQUEST_APPROVED,
    INSTRUCTOR_REQUEST_REJECTED,
    /** Pour les administrateurs : un cours a été soumis pour validation (objet = titre, détail = formateur). */
    COURSE_SUBMITTED,
    /** Pour le formateur : son cours est validé et publié (objet = titre). */
    COURSE_APPROVED,
    /** Pour le formateur : son cours est refusé (objet = titre, détail = motif). */
    COURSE_REJECTED
}
