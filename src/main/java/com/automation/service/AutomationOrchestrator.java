package com.automation.service;

import com.automation.dto.AutomationRequest;
import com.automation.dto.AutomationStatus;
import com.automation.model.AutomationRun;
import com.automation.repository.AutomationRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AutomationOrchestrator {

    @Autowired
    private AutomationRunRepository automationRunRepository;

    @Autowired
    private QuotaManager quotaManager;

    @Autowired
    private EnhancedApplicationAutomationService enhancedApplicationService;

    private final ConcurrentHashMap<String, AutomationStatus> activeAutomations = new ConcurrentHashMap<>();

    public AutomationStatus startAutomation(AutomationRequest request) {
        String automationId = UUID.randomUUID().toString();
        
        // Create automation run record
        AutomationRun run = new AutomationRun();
        run.setRunId(automationId);
        run.setUserId(request.getUserId());
        run.setStatus("RUNNING");
        run.setPlatforms(String.join(",", request.getPlatforms()));
        run.setStartedAt(LocalDateTime.now());
        automationRunRepository.save(run);

        // Create status
        AutomationStatus status = new AutomationStatus();
        status.setAutomationId(automationId);
        status.setStatus("RUNNING");
        status.setCurrentPlatform(request.getPlatforms().get(0));
        status.setStartedAt(LocalDateTime.now());
        status.setLastActivityAt(LocalDateTime.now());
        status.setProgress(new AutomationStatus.Progress(0, 0, 0, 0));

        // Store in active automations
        activeAutomations.put(automationId, status);

        // Start automation in background thread
        Thread automationThread = new Thread(() -> runAutomation(automationId, request));
        automationThread.start();

        return status;
    }

    public void stopAutomation(String automationId) {
        AutomationStatus status = activeAutomations.get(automationId);
        if (status != null) {
            status.setStatus("STOPPED");
            
            // Update database
            AutomationRun run = automationRunRepository.findByRunId(automationId).orElse(null);
            if (run != null) {
                run.setStatus("STOPPED");
                run.setCompletedAt(LocalDateTime.now());
                automationRunRepository.save(run);
            }
            
            activeAutomations.remove(automationId);
        }
    }

    public AutomationStatus getAutomationStatus(String automationId) {
        return activeAutomations.getOrDefault(automationId, createDefaultStatus(automationId));
    }

    private void runAutomation(String automationId, AutomationRequest request) {
        try {
            AutomationStatus status = activeAutomations.get(automationId);
            if (status == null) return;

            // Process each platform
            for (String platform : request.getPlatforms()) {
                status.setCurrentPlatform(platform);
                status.setLastActivityAt(LocalDateTime.now());

                // Check quota availability
                if (quotaManager.isQuotaAvailable(request.getUserId(), platform)) {
                    // Process jobs for this platform
                    // This is a placeholder - actual implementation would call platform-specific services
                    processPlatformJobs(automationId, platform, request.getUserId());
                } else {
                    // Skip platform if quota not available
                    continue;
                }
            }

            // Mark as completed
            status.setStatus("COMPLETED");
            AutomationRun run = automationRunRepository.findByRunId(automationId).orElse(null);
            if (run != null) {
                run.setStatus("COMPLETED");
                run.setCompletedAt(LocalDateTime.now());
                automationRunRepository.save(run);
            }

            activeAutomations.remove(automationId);

        } catch (Exception e) {
            // Handle error
            AutomationStatus status = activeAutomations.get(automationId);
            if (status != null) {
                status.setStatus("FAILED");
                
                AutomationRun run = automationRunRepository.findByRunId(automationId).orElse(null);
                if (run != null) {
                    run.setStatus("FAILED");
                    run.setCompletedAt(LocalDateTime.now());
                    run.setErrorMessage(e.getMessage());
                    automationRunRepository.save(run);
                }
                
                activeAutomations.remove(automationId);
            }
        }
    }

    private void processPlatformJobs(String automationId, String platform, Long userId) {
        // Enhanced platform-specific job processing with question answering
        try {
            log.info("Processing jobs for platform: {}", platform);
            
            // This would integrate with job queue processing
            // For each job in the queue, call the enhanced application service
            // Example implementation:
            /*
            List<JobQueueItem> pendingJobs = jobQueueRepository.findByUserIdAndPlatformAndStatus(userId, platform, "PENDING");
            
            for (JobQueueItem job : pendingJobs) {
                try {
                    Map<String, Object> jobData = new HashMap<>();
                    jobData.put("jobTitle", job.getJobTitle());
                    jobData.put("company", job.getCompany());
                    jobData.put("matchScore", job.getMatchScore());
                    
                    EnhancedApplicationAutomationService.ApplicationResult result = 
                        enhancedApplicationService.processApplication(platform, job.getJobUrl(), userId, jobData);
                    
                    if (result.isSuccess()) {
                        job.setStatus("COMPLETED");
                        job.setProcessedAt(LocalDateTime.now());
                    } else {
                        job.setStatus("FAILED");
                        job.setErrorMessage(result.getFailureReason());
                    }
                    
                    jobQueueRepository.save(job);
                    
                    // Update automation status
                    AutomationStatus status = activeAutomations.get(automationId);
                    if (status != null) {
                        status.getProgress().jobsApplied++;
                        status.setLastActivityAt(LocalDateTime.now());
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing job: {}", job.getJobUrl(), e);
                    job.setStatus("FAILED");
                    job.setErrorMessage(e.getMessage());
                    jobQueueRepository.save(job);
                }
            }
            */
            
            // Placeholder for demonstration
            Thread.sleep(1000); // Simulate processing time
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error processing platform jobs for platform: {}", platform, e);
        }
    }

    private AutomationStatus createDefaultStatus(String automationId) {
        AutomationStatus status = new AutomationStatus();
        status.setAutomationId(automationId);
        status.setStatus("NOT_FOUND");
        return status;
    }
}
