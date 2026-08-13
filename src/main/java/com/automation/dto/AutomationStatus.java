package com.automation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationStatus {

    private String automationId;
    private String status;
    private String currentPlatform;
    private Progress progress;
    private Map<String, QuotaInfo> quota;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivityAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private int jobsFound;
        private int jobsApplied;
        private int jobsFailed;
        private int jobsSkipped;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaInfo {
        private int dailyLimit;
        private int appliedToday;
        private int remaining;
        private String status;
    }
}
