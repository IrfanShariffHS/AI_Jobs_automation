package com.automation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "performance_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "jobs_processed")
    private Integer jobsProcessed = 0;

    @Column(name = "jobs_applied")
    private Integer jobsApplied = 0;

    @Column(name = "jobs_failed")
    private Integer jobsFailed = 0;

    @Column(name = "avg_match_score")
    private BigDecimal avgMatchScore;

    @Column(name = "avg_processing_time_ms")
    private Integer avgProcessingTimeMs;

    @Column(name = "quota_utilization")
    private BigDecimal quotaUtilization;
}
