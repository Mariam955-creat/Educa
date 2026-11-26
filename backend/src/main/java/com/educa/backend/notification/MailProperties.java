package com.educa.backend.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Serveur SMTP des emails transactionnels ({@code educa.mail.*}) ; {@code host} vide = emails journalisés. */
@ConfigurationProperties(prefix = "educa.mail")
public record MailProperties(String host, int port, String username, String password, String from, boolean starttls) {
}
