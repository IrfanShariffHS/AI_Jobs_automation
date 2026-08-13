package com.automation.repository;

import com.automation.model.ApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, Long> {

    List<ApplicationHistory> findByUserId(Long userId);

    List<ApplicationHistory> findByUserIdAndPlatform(Long userId, String platform);

    List<ApplicationHistory> findByUserIdAndApplicationStatus(Long userId, String applicationStatus);

    List<ApplicationHistory> findByUserIdAndPlatformAndAppliedAtBetween(
        Long userId, String platform, LocalDateTime start, LocalDateTime end
    );
}
