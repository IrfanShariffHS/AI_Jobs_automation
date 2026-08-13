package com.automation.service;

import com.automation.dto.QuotaStatus;
import com.automation.model.QuotaTracking;
import com.automation.repository.QuotaTrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class QuotaManager {

    @Autowired
    private QuotaTrackingRepository quotaTrackingRepository;

    @Autowired
    private BackendApiClient backendApiClient;

    public QuotaStatus getQuotaStatus(Long userId, String platform) {
        LocalDate today = LocalDate.now();
        Optional<QuotaTracking> tracking = quotaTrackingRepository
            .findByUserIdAndPlatformAndQuotaDate(userId, platform, today);

        if (tracking.isEmpty()) {
            // Initialize quota for today
            return initializeQuota(userId, platform, today);
        }

        QuotaTracking quota = tracking.get();
        return mapToQuotaStatus(quota);
    }

    public boolean isQuotaAvailable(Long userId, String platform) {
        QuotaStatus status = getQuotaStatus(userId, platform);
        return "AVAILABLE".equals(status.getQuotaStatus()) && status.getRemaining() > 0;
    }

    public void recordApplication(Long userId, String platform) {
        LocalDate today = LocalDate.now();
        Optional<QuotaTracking> tracking = quotaTrackingRepository
            .findByUserIdAndPlatformAndQuotaDate(userId, platform, today);

        if (tracking.isPresent()) {
            QuotaTracking quota = tracking.get();
            quota.setApplicationsSubmitted(quota.getApplicationsSubmitted() + 1);
            quota.setLastUpdated(LocalDateTime.now());

            // Check if quota reached
            if (quota.getApplicationsSubmitted() >= quota.getDailyLimit()) {
                quota.setQuotaStatus("QUOTA_REACHED");
                quota.setResetTime(LocalDate.now().plusDays(1).atStartOfDay());
            }

            quotaTrackingRepository.save(quota);
        }
    }

    public void resetQuota(Long userId, String platform) {
        LocalDate today = LocalDate.now();
        Optional<QuotaTracking> tracking = quotaTrackingRepository
            .findByUserIdAndPlatformAndQuotaDate(userId, platform, today);

        if (tracking.isPresent()) {
            QuotaTracking quota = tracking.get();
            quota.setApplicationsSubmitted(0);
            quota.setQuotaStatus("AVAILABLE");
            quota.setResetTime(null);
            quota.setLastUpdated(LocalDateTime.now());
            quotaTrackingRepository.save(quota);
        }
    }

    private QuotaStatus initializeQuota(Long userId, String platform, LocalDate date) {
        // Fetch default quota from backend settings
        Integer dailyLimit = getDefaultDailyLimit(userId, platform);
        
        QuotaTracking quota = new QuotaTracking();
        quota.setUserId(userId);
        quota.setPlatform(platform);
        quota.setQuotaDate(date);
        quota.setDailyLimit(dailyLimit);
        quota.setApplicationsSubmitted(0);
        quota.setQuotaStatus("AVAILABLE");
        quota.setLastUpdated(LocalDateTime.now());

        quotaTrackingRepository.save(quota);
        return mapToQuotaStatus(quota);
    }

    private Integer getDefaultDailyLimit(Long userId, String platform) {
        try {
            if ("naukri".equalsIgnoreCase(platform)) {
                Map<String, Object> settings = backendApiClient.getNaukriSettings(userId);
                Object limit = settings.get("dailyApplicationLimit");
                return limit != null ? ((Number) limit).intValue() : 50;
            } else if ("linkedin".equalsIgnoreCase(platform)) {
                Map<String, Object> settings = backendApiClient.getLinkedInSettings(userId);
                Object limit = settings.get("autoApplyLimitPerDay");
                return limit != null ? ((Number) limit).intValue() : 20;
            }
        } catch (Exception e) {
            // Fallback to default values
        }
        return "naukri".equalsIgnoreCase(platform) ? 50 : 20;
    }

    private QuotaStatus mapToQuotaStatus(QuotaTracking quota) {
        QuotaStatus status = new QuotaStatus();
        status.setPlatform(quota.getPlatform());
        status.setUserId(quota.getUserId());
        status.setQuotaDate(quota.getQuotaDate().toString());
        status.setDailyLimit(quota.getDailyLimit());
        status.setApplicationsSubmitted(quota.getApplicationsSubmitted());
        status.setRemaining(quota.getDailyLimit() - quota.getApplicationsSubmitted());
        status.setQuotaStatus(quota.getQuotaStatus());
        status.setResetTime(quota.getResetTime());
        status.setLastUpdated(quota.getLastUpdated());
        return status;
    }
}
