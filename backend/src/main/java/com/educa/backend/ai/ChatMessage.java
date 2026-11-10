package com.educa.backend.ai;

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

/** Un message de la conversation d'un apprenant avec le chatbot d'un cours. */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    public enum Role {
        USER,
        ASSISTANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Couplage par id avec les modules {@code user} et {@code course}, pas de relation JPA. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** Réponse de repli (IA indisponible) : affichée, jamais renvoyée au modèle. */
    @Column(nullable = false)
    private boolean degraded;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static ChatMessage of(Long userId, Long courseId, Role role, String content, boolean degraded) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setCourseId(courseId);
        message.setRole(role);
        message.setContent(content);
        message.setDegraded(degraded);
        return message;
    }
}
