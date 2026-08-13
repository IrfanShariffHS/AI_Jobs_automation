package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "job_url", nullable = false)
    private String jobUrl;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company")
    private String company;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "application_status", nullable = false)
    private String applicationStatus; // PENDING, IN_PROGRESS, SUBMITTED, FAILED, SKIPPED, QUOTA_REACHED

    @Column(name = "submission_status")
    private String submissionStatus; // SUCCESS, FAILED, CAPTCHA_REQUIRED, RATE_LIMITED

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
