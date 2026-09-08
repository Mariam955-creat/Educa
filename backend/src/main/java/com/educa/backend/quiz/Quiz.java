package com.educa.backend.quiz;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** Renseigné si {@code type = CONTROL}, {@code null} si {@code FINAL_EXAM}. */
    @Column(name = "chapter_id")
    private Long chapterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private QuizType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "pass_threshold", nullable = false)
    private int passThreshold = 50;

    /** {@code null} = illimité (CONTROL) ; ex. 3 pour FINAL_EXAM. */
    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        question.setQuiz(this);
        this.questions.add(question);
    }
}
