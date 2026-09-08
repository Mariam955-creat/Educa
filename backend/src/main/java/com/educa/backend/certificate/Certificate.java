package com.educa.backend.certificate;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "certificates", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "course_id" }))
@Getter
@Setter
@NoArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "final_exam_attempt_id", nullable = false)
    private Long finalExamAttemptId;

    @Column(name = "controls_average", nullable = false, precision = 5, scale = 2)
    private BigDecimal controlsAverage;

    @Column(name = "final_exam_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalExamScore;

    @Column(name = "final_grade", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalGrade;

    @Column(name = "serial_number", nullable = false, unique = true, length = 40)
    private String serialNumber;

    @Column(name = "verification_code", nullable = false, unique = true, length = 64)
    private String verificationCode;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "pdf_key", length = 500)
    private String pdfKey;
}
