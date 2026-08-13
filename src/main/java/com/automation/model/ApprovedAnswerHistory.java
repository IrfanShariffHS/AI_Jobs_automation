package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for storing approved answers that can be reused for similar questions
 * across different applications and platforms
 */
@Entity
@Table(name = "approved_answer_history", indexes = {
    @Index(name = "idx_user_type", columnList = "user_id, question_type"),
    @Index(name = "idx_usage", columnList = "usage_count")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovedAnswerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void incrementUsage() {
        this.usageCount = (this.usageCount == null) ? 1 : this.usageCount + 1;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void incrementSuccess() {
        this.successCount = (this.successCount == null) ? 1 : this.successCount + 1;
    }

    public double getSuccessRate() {
        if (usageCount == null || usageCount == 0) {
            return 0.0;
        }
        return (double) successCount / usageCount;
    }
}