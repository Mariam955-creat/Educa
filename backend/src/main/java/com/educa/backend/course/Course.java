package com.educa.backend.course;

import java.math.BigDecimal;
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
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id du formateur propriétaire (module {@code user}). Pas de relation JPA : couplage par id. */
    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 2)
    private String language = "fr";

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "control_weight", nullable = false)
    private int controlWeight = 40;

    @Column(name = "exam_weight", nullable = false)
    private int examWeight = 60;

    @Column(name = "pass_threshold", nullable = false)
    private int passThreshold = 70;

    /** Prix d'achat unique du cours, fixé par le formateur. {@code 0} = cours gratuit. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    // ---------- page de présentation ----------

    /** Accroche affichée sous le titre. */
    @Column(length = 200)
    private String subtitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CourseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CourseLevel level;

    /** Durée estimée pour suivre le cours, en heures. */
    @Column(name = "duration_hours", precision = 5, scale = 1)
    private BigDecimal durationHours;

    /** « Ce que vous apprendrez » : une entrée par ligne. */
    @Column(columnDefinition = "text")
    private String objectives;

    /** Prérequis : une entrée par ligne. */
    @Column(columnDefinition = "text")
    private String prerequisites;

    @Column(name = "target_audience", columnDefinition = "text")
    private String targetAudience;

    /** Clé de stockage de l'image de couverture (module {@code storage}), {@code null} si aucune. */
    @Column(name = "cover_image_key", length = 500)
    private String coverImageKey;

    /** Type MIME détecté de l'image de couverture. */
    @Column(name = "cover_image_type", length = 100)
    private String coverImageType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Chapter> chapters = new ArrayList<>();

    public void addChapter(Chapter chapter) {
        chapter.setCourse(this);
        this.chapters.add(chapter);
    }
}
