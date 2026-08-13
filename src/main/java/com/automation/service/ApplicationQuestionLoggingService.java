package com.automation.service;

import com.automation.model.ApplicationQuestionHistory;
import com.automation.repository.ApplicationQuestionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for comprehensive logging and analytics of application question processing
 */
@Service
public class ApplicationQuestionLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationQuestionLoggingService.class);

    @Autowired
    private ApplicationQuestionHistoryRepository questionHistoryRepository;

    /**
     * Log detailed question processing information
     */
    @Transactional
    public void logQuestionProcessing(ApplicationQuestionHistory history) {
        try {
            questionHistoryRepository.save(history);
            
            log.info("Question processed - User: {}, Platform: {}, Type: {}, Confidence: {}, Source: {}, Filled: {}",
                    history.getUserId(),
                    history.getPlatform(),
                    history.getQuestionType(),
                    history.getConfidenceScore(),
                    history.getAnswerSource(),
                    history.getSuccessfullyFilled());

        } catch (Exception e) {
            log.error("Error logging question processing", e);
        }
    }

    /**
     * Log validation errors
     */
    @Transactional
    public void logValidationError(Long userId, String platform, String fieldIdentifier, String error) {
        try {
            ApplicationQuestionHistory history = new ApplicationQuestionHistory();
            history.setUserId(userId);
            history.setPlatform(platform);
            history.setQuestionText("VALIDATION_ERROR");
            history.setNormalizedQuestion("validation_error");
            history.setQuestionType("UNKNOWN");
            history.setAnswer("");
            history.setAnswerSource("SYSTEM");
            history.setConfidenceScore(java.math.BigDecimal.ZERO);
            history.setFieldType("UNKNOWN");
            history.setFieldIdentifier(fieldIdentifier);
            history.setIsRequired(false);
            history.setSuccessfullyFilled(false);
            history.setValidationError(error);
            history.setCreatedAt(LocalDateTime.now());

            questionHistoryRepository.save(history);

            log.warn("Validation error logged - User: {}, Platform: {}, Field: {}, Error: {}",
                    userId, platform, fieldIdentifier, error);

        } catch (Exception e) {
            log.error("Error logging validation error", e);
        }
    }

    /**
     * Get question processing statistics for a user
     */
    public Map<String, Object> getUserQuestionStatistics(Long userId) {
        try {
            List<ApplicationQuestionHistory> userHistory = questionHistoryRepository
                    .findByUserIdAndPlatform(userId, "naukri"); // This would need to be adjusted for all platforms

            Map<String, Object> stats = new java.util.HashMap<>();
            
            stats.put("totalQuestions", userHistory.size());
            stats.put("successfullyFilled", userHistory.stream()
                    .filter(ApplicationQuestionHistory::getSuccessfullyFilled)
                    .count());
            stats.put("failedQuestions", userHistory.stream()
                    .filter(h -> !h.getSuccessfullyFilled())
                    .count());
            
            Map<String, Long> questionsByType = userHistory.stream()
                    .collect(Collectors.groupingBy(ApplicationQuestionHistory::getQuestionType, Collectors.counting()));
            stats.put("questionsByType", questionsByType);
            
            Map<String, Long> questionsBySource = userHistory.stream()
                    .collect(Collectors.groupingBy(ApplicationQuestionHistory::getAnswerSource, Collectors.counting()));
            stats.put("questionsBySource", questionsBySource);
            
            double avgConfidence = userHistory.stream()
                    .filter(h -> h.getConfidenceScore() != null)
                    .mapToDouble(h -> h.getConfidenceScore().doubleValue())
                    .average()
                    .orElse(0.0);
            stats.put("averageConfidence", avgConfidence);

            return stats;

        } catch (Exception e) {
            log.error("Error getting user question statistics", e);
            return Map.of("error", "Failed to retrieve statistics");
        }
    }

    /**
     * Get recent question processing history
     */
    public List<ApplicationQuestionHistory> getRecentQuestionHistory(Long userId, int limit) {
        try {
            // This would need a proper query in the repository
            return questionHistoryRepository.findByUserIdAndPlatform(userId, "naukri")
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting recent question history", e);
            return List.of();
        }
    }

    /**
     * Log application workflow start
     */
    public void logWorkflowStart(Long userId, String platform, String jobUrl) {
        log.info("Workflow started - User: {}, Platform: {}, JobUrl: {}, Timestamp: {}",
                userId, platform, jobUrl, LocalDateTime.now());
    }

    /**
     * Log application workflow completion
     */
    public void logWorkflowComplete(Long userId, String platform, String jobUrl, boolean success, String reason) {
        log.info("Workflow completed - User: {}, Platform: {}, JobUrl: {}, Success: {}, Reason: {}, Timestamp: {}",
                userId, platform, jobUrl, success, reason, LocalDateTime.now());
    }

    /**
     * Log CAPTCHA detection
     */
    public void logCaptchaDetection(Long userId, String platform, String jobUrl) {
        log.warn("CAPTCHA detected - User: {}, Platform: {}, JobUrl: {}, Timestamp: {}",
                userId, platform, jobUrl, LocalDateTime.now());
    }

    /**
     * Log quota limit reached
     */
    public void logQuotaLimitReached(Long userId, String platform) {
        log.warn("Quota limit reached - User: {}, Platform: {}, Timestamp: {}",
                userId, platform, LocalDateTime.now());
    }

    /**
     * Log user confirmation required
     */
    public void logUserConfirmationRequired(Long userId, String platform, String jobUrl, String reason) {
        log.info("User confirmation required - User: {}, Platform: {}, JobUrl: {}, Reason: {}, Timestamp: {}",
                userId, platform, jobUrl, reason, LocalDateTime.now());
    }

    /**
     * Log auto-submit decision
     */
    public void logAutoSubmitDecision(Long userId, String platform, String jobUrl, boolean canAutoSubmit, String reason) {
        log.info("Auto-submit decision - User: {}, Platform: {}, JobUrl: {}, CanAutoSubmit: {}, Reason: {}, Timestamp: {}",
                userId, platform, jobUrl, canAutoSubmit, reason, LocalDateTime.now());
    }

    /**
     * Log submission verification result
     */
    public void logSubmissionVerification(Long userId, String platform, String jobUrl, boolean verified, String reason) {
        log.info("Submission verification - User: {}, Platform: {}, JobUrl: {}, Verified: {}, Reason: {}, Timestamp: {}",
                userId, platform, jobUrl, verified, reason, LocalDateTime.now());
    }

    /**
     * Log error with context
     */
    public void logError(Long userId, String platform, String context, String error, Exception exception) {
        log.error("Error occurred - User: {}, Platform: {}, Context: {}, Error: {}, Exception: {}",
                userId, platform, context, error, exception != null ? exception.getMessage() : "None", exception);
    }

    /**
     * Create diagnostic log entry
     */
    @Transactional
    public void createDiagnosticLog(Long userId, String platform, String diagnosticInfo) {
        try {
            ApplicationQuestionHistory history = new ApplicationQuestionHistory();
            history.setUserId(userId);
            history.setPlatform(platform);
            history.setQuestionText("DIAGNOSTIC_INFO");
            history.setNormalizedQuestion("diagnostic_info");
            history.setQuestionType("SYSTEM");
            history.setAnswer(diagnosticInfo);
            history.setAnswerSource("SYSTEM");
            history.setConfidenceScore(java.math.BigDecimal.ONE);
            history.setFieldType("SYSTEM");
            history.setFieldIdentifier("DIAGNOSTIC");
            history.setIsRequired(false);
            history.setSuccessfullyFilled(true);
            history.setCreatedAt(LocalDateTime.now());

            questionHistoryRepository.save(history);

            log.debug("Diagnostic log created - User: {}, Platform: {}, Info: {}", userId, platform, diagnosticInfo);

        } catch (Exception e) {
            log.error("Error creating diagnostic log", e);
        }
    }
}