package com.automation.service;

import com.automation.model.JobQueueItem;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobPrioritizer {

    public List<JobQueueItem> prioritizeJobs(List<JobQueueItem> jobs, int minMatchScore) {
        return jobs.stream()
            .filter(job -> job.getMatchScore() != null && job.getMatchScore() >= minMatchScore)
            .sorted(Comparator
                .comparing(JobQueueItem::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(JobQueueItem::getQueuedAt, Comparator.reverseOrder())
                .thenComparing(JobQueueItem::getPriority, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    public List<JobQueueItem> selectTopJobs(List<JobQueueItem> jobs, int count) {
        return jobs.stream()
            .limit(count)
            .collect(Collectors.toList());
    }
}
