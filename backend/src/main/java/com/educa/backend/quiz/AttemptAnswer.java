package com.educa.backend.quiz;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attempt_answers")
@Getter
@Setter
@NoArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Options cochées par l'apprenant (colonne PostgreSQL {@code bigint[]}). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "selected_option_ids", columnDefinition = "bigint[]", nullable = false)
    private Long[] selectedOptionIds = new Long[0];

    @Column(nullable = false)
    private boolean correct;
}
