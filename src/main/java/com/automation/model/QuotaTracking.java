package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quota_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "platform", nullable = false)
    private String platform; // naukri, linkedin

    @Column(name = "quota_date", nullable = false)
    private LocalDate quotaDate;

    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    @Column(name = "applications_submitted", nullable = false)
    private Integer applicationsSubmitted = 0;

    @Column(name = "quota_status", nullable = false)
    private String quotaStatus; // AVAILABLE, QUOTA_REACHED, RATE_LIMITED

    @Column(name = "reset_time")
    private LocalDateTime resetTime;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
