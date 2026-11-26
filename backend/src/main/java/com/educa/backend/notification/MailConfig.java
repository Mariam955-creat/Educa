package com.educa.backend.notification;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * Choisit l'envoi des emails : SMTP quand {@code educa.mail.host} est renseigné, sinon un repli qui écrit
 * l'email dans le journal (même principe que les replis de l'IA et du paiement).
 */
@Configuration(proxyBeanMethods = false)
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    EmailSender emailSender(MailProperties properties) {
        if (!StringUtils.hasText(properties.host())) {
            log.info("Emails : aucun serveur SMTP (MAIL_HOST vide), les emails seront écrits dans le journal");
            return (to, subject, body) -> log.info("[email non envoyé, pas de SMTP] à={} sujet=« {} »\n{}", to, subject, body);
        }
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(properties.host());
        mailSender.setPort(properties.port());
        if (StringUtils.hasText(properties.username())) {
            mailSender.setUsername(properties.username());
            mailSender.setPassword(properties.password());
        }
        Properties javaMail = mailSender.getJavaMailProperties();
        javaMail.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(properties.username())));
        javaMail.put("mail.smtp.starttls.enable", String.valueOf(properties.starttls()));
        javaMail.put("mail.smtp.connectiontimeout", "5000");
        javaMail.put("mail.smtp.timeout", "5000");
        javaMail.put("mail.smtp.writetimeout", "5000");
        mailSender.setDefaultEncoding("UTF-8");
        return (to, subject, body) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        };
    }
}
