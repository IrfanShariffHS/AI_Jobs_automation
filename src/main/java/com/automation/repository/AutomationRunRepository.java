package com.automation.repository;

import com.automation.model.AutomationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutomationRunRepository extends JpaRepository<AutomationRun, Long> {

    Optional<AutomationRun> findByRunId(String runId);

    List<AutomationRun> findByUserId(Long userId);

    List<AutomationRun> findByUserIdAndStatus(Long userId, String status);

    Optional<AutomationRun> findFirstByUserIdOrderByStartedAtDesc(Long userId);
}
