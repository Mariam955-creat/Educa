package com.educa.backend.course;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Traduction du titre d'un chapitre dans une langue donnée. */
@Entity
@Table(name = "chapter_translations", uniqueConstraints = @UniqueConstraint(columnNames = { "chapter_id", "language_code" }))
@Getter
@Setter
@NoArgsConstructor
public class ChapterTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "language_code", nullable = false, length = 2)
    private String languageCode;

    @Column(nullable = false, length = 200)
    private String title;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
