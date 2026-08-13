package com.automation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaStatus {

    private String platform;
    private Long userId;
    private String quotaDate;
    private Integer dailyLimit;
    private Integer applicationsSubmitted;
    private Integer remaining;
    private String quotaStatus;
    private LocalDateTime resetTime;
    private LocalDateTime lastUpdated;
}
