package com.automation.repository;

import com.automation.model.QuotaTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotaTrackingRepository extends JpaRepository<QuotaTracking, Long> {

    Optional<QuotaTracking> findByUserIdAndPlatformAndQuotaDate(Long userId, String platform, LocalDate quotaDate);

    List<QuotaTracking> findByUserIdAndPlatform(Long userId, String platform);

    List<QuotaTracking> findByUserIdAndQuotaDate(Long userId, LocalDate quotaDate);

    List<QuotaTracking> findByQuotaDateBefore(LocalDate date);
}
