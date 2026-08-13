package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for storing semantic question mappings to enable cross-platform
 * question matching with different wordings
 */
@Entity
@Table(name = "semantic_question_mappings", indexes = {
    @Index(name = "idx_user_type", columnList = "user_id, question_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_canonical", columnNames = {"user_id", "canonical_question"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticQuestionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "canonical_question", nullable = false, columnDefinition = "TEXT")
    private String canonicalQuestion;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Column(name = "variants", columnDefinition = "JSON")
    private String variants;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}