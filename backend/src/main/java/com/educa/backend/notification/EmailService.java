package com.educa.backend.notification;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * Emails transactionnels rédigés dans la langue du destinataire (fichiers {@code messages_*.properties},
 * clés {@code mail.<modèle>.subject} et {@code mail.<modèle>.body}). Un échec d'envoi est journalisé, jamais
 * propagé : il ne doit pas annuler l'action qui l'a déclenché.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender sender;
    private final MessageSource messages;

    public EmailService(EmailSender sender, MessageSource messages) {
        this.sender = sender;
        this.messages = messages;
    }

    public void send(String to, String language, String template, Object... args) {
        Locale locale = Locale.forLanguageTag(language != null ? language : "fr");
        try {
            String subject = messages.getMessage("mail." + template + ".subject", args, locale);
            String body = messages.getMessage("mail." + template + ".body", args, locale);
            sender.send(to, subject, body);
        } catch (RuntimeException e) {
            log.warn("Email « {} » non envoyé à {} : {}", template, to, e.getMessage());
        }
    }
}
