package com.educa.backend.notification;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Couplage par id avec le module {@code user}, pas de relation JPA. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    /** Objet inséré dans le texte traduit (ex. titre d'un cours) ; facultatif. */
    @Column(length = 255)
    private String subject;

    /** Information libre affichée avec le texte (ex. motif d'un refus) ; facultative. */
    @Column(columnDefinition = "text")
    private String detail;

    /** Route de l'application ouverte au clic (ex. {@code /instructor}) ; facultative. */
    @Column(length = 255)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
