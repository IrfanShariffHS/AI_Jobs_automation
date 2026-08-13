package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "retry_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_history_id", nullable = false)
    private Long applicationHistoryId;

    @Column(name = "retry_attempt", nullable = false)
    private Integer retryAttempt;

    @Column(name = "retry_reason")
    private String retryReason;

    @Column(name = "retry_at", nullable = false)
    private LocalDateTime retryAt;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message")
    private String errorMessage;
}
