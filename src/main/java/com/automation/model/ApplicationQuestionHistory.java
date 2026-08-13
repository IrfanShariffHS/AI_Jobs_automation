package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for storing detailed history of application questions and answers
 * across all platforms (Naukri, LinkedIn, Indeed, etc.)
 */
@Entity
@Table(name = "application_question_history", indexes = {
    @Index(name = "idx_user_platform", columnList = "user_id, platform"),
    @Index(name = "idx_question_type", columnList = "question_type"),
    @Index(name = "idx_confidence", columnList = "confidence_score"),
    @Index(name = "idx_job_url", columnList = "job_url")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationQuestionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "normalized_question", nullable = false, columnDefinition = "TEXT")
    private String normalizedQuestion;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answer_source")
    private String answerSource;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "field_type")
    private String fieldType;

    @Column(name = "field_identifier")
    private String fieldIdentifier;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    public void setRequired(Boolean required) {
        this.isRequired = required;
    }
    public Boolean getRequired() {
        return this.isRequired;
    }

    @Column(name = "successfully_filled")
    private Boolean successfullyFilled = false;

    @Column(name = "validation_error", columnDefinition = "TEXT")
    private String validationError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AnswerSource {
        USER_PROFILE,
        RESUME_DATA,
        JOB_PREFERENCES,
        PREVIOUSLY_APPROVED,
        AI_GENERATED,
        USER_INPUT,
        UNKNOWN
    }

    public enum QuestionType {
        NOTICE_PERIOD,
        CURRENT_CTC,
        EXPECTED_CTC,
        TOTAL_EXPERIENCE,
        SKILL_EXPERIENCE,
        EDUCATION,
        LOCATION,
        RELOCATION,
        WORK_AUTHORIZATION,
        VISA_STATUS,
        JOINING_DATE,
        PHONE,
        EMAIL,
        YES_NO,
        MULTIPLE_CHOICE,
        NUMBER,
        DATE,
        TEXT,
        JOB_SPECIFIC,
        UNKNOWN
    }

    public enum FieldType {
        TEXT_INPUT,
        TEXTAREA,
        RADIO_BUTTON,
        CHECKBOX,
        DROPDOWN,
        SELECT,
        DATE_FIELD,
        NUMERIC_FIELD,
        AUTOCOMPLETE,
        UNKNOWN
    }
}