package com.educa.backend.notification;

/** Envoi d'un email texte. Implémentations : SMTP si configuré, sinon écriture dans le journal. */
public interface EmailSender {

    void send(String to, String subject, String body);
}
