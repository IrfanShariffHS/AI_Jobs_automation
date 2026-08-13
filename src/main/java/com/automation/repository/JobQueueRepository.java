package com.automation.repository;

import com.automation.model.JobQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobQueueRepository extends JpaRepository<JobQueueItem, Long> {

    List<JobQueueItem> findByUserIdAndStatus(Long userId, String status);

    List<JobQueueItem> findByUserIdAndPlatformAndStatus(Long userId, String platform, String status);

    List<JobQueueItem> findByUserIdOrderByPriorityDescQueuedAtDesc(Long userId);

    List<JobQueueItem> findByUserIdAndPlatformOrderByPriorityDescQueuedAtDesc(Long userId, String platform);
}
