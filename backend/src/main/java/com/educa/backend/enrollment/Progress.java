package com.educa.backend.enrollment;

import java.time.Instant;

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

@Entity
@Table(name = "progress", uniqueConstraints = @UniqueConstraint(columnNames = { "enrollment_id", "content_id" }))
@Getter
@Setter
@NoArgsConstructor
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();
}
