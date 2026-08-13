package com.automation.service;

import com.automation.dto.QuestionAnswerPair;
import com.automation.dto.QuestionProcessingResult;
import com.automation.model.ApplicationHistory;
import com.automation.repository.ApplicationHistoryRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Enhanced Application Automation Service
 * Integrates QuestionAnswerEngine with Playwright automation for complete application workflow
 */
@Service
public class EnhancedApplicationAutomationService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedApplicationAutomationService.class);

    @Autowired
    private QuestionAnswerEngine questionAnswerEngine;

    @Autowired
    private ApplicationHistoryRepository applicationHistoryRepository;

    @Autowired
    private BackendApiClient backendApiClient;

    @Autowired
    private QuotaManager quotaManager;

    /**
     * Complete application workflow with question answering
     */
    @Transactional
    public ApplicationResult processApplication(String platform, String jobUrl, Long userId, 
                                               Map<String, Object> jobData) {
        log.info("Starting enhanced application processing for user {} on platform {}", userId, platform);

        ApplicationResult result = new ApplicationResult();
        result.setUserId(userId);
        result.setPlatform(platform);
        result.setJobUrl(jobUrl);
        result.setStartTime(LocalDateTime.now());

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            // Check quota availability
            if (!quotaManager.isQuotaAvailable(userId, platform)) {
                result.setSuccess(false);
                result.setFailureReason("Quota limit reached for platform: " + platform);
                result.setApplicationStatus("QUOTA_REACHED");
                log.warn("Quota limit reached for user {} on platform {}", userId, platform);
                return result;
            }

            // Initialize Playwright
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            context = browser.newContext();
            page = context.newPage();

            // Navigate to job application page
            log.info("Navigating to job URL: {}", jobUrl);
            page.navigate(jobUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);

            // Detect and handle CAPTCHA
            if (detectCaptcha(page)) {
                result.setSuccess(false);
                result.setFailureReason("CAPTCHA detected - automation stopped");
                result.setApplicationStatus("CAPTCHA_REQUIRED");
                log.warn("CAPTCHA detected on job page: {}", jobUrl);
                return result;
            }

            // Process application form with question answering
            log.info("Processing application form questions");
            QuestionProcessingResult questionResult = questionAnswerEngine.processApplicationForm(page, platform, jobUrl, userId);
            result.setQuestionProcessingResult(questionResult);

            // Determine if we can auto-submit
            boolean canAutoSubmit = determineAutoSubmitEligibility(questionResult);
            result.setCanAutoSubmit(canAutoSubmit);

            if (canAutoSubmit) {
                // Submit application
                log.info("Auto-submitting application");
                boolean submitted = submitApplication(page, platform);
                result.setSuccess(submitted);

                if (submitted) {
                    // Verify submission
                    boolean verified = verifySubmission(page, platform);
                    result.setSubmissionVerified(verified);

                    if (verified) {
                        result.setApplicationStatus("SUBMITTED");
                        result.setSubmissionStatus("SUCCESS");
                        
                        // Update quota
                        quotaManager.recordApplication(userId, platform);
                        
                        // Create application record in backend
                        createBackendApplicationRecord(userId, platform, jobUrl, jobData);
                        
                        log.info("Application successfully submitted and verified for user {} on platform {}", userId, platform);
                    } else {
                        result.setApplicationStatus("IN_PROGRESS");
                        result.setFailureReason("Submission verification failed");
                        log.warn("Application submission verification failed for user {} on platform {}", userId, platform);
                    }
                } else {
                    result.setApplicationStatus("FAILED");
                    result.setFailureReason("Failed to submit application");
                    result.setSubmissionStatus("FAILED");
                    log.error("Failed to submit application for user {} on platform {}", userId, platform);
                }
            } else {
                result.setApplicationStatus("REQUIRES_USER_CONFIRMATION");
                result.setFailureReason("Application requires user confirmation due to low confidence answers");
                log.info("Application requires user confirmation for user {} on platform {}", userId, platform);
            }

        } catch (Exception e) {
            log.error("Error during application processing", e);
            result.setSuccess(false);
            result.setApplicationStatus("FAILED");
            result.setFailureReason("Error: " + e.getMessage());
            result.setSubmissionStatus("FAILED");
        } finally {
            // Cleanup resources
            if (page != null) page.close();
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();

            result.setEndTime(LocalDateTime.now());
            
            // Save application history
            saveApplicationHistory(result);
        }

        return result;
    }

    /**
     * Detect CAPTCHA on the page
     */
    private boolean detectCaptcha(Page page) {
        try {
            // Common CAPTCHA indicators
            String[] captchaSelectors = {
                "iframe[src*='recaptcha']",
                "iframe[src*='captcha']",
                ".g-recaptcha",
                "#captcha",
                "[class*='captcha']",
                "[id*='captcha']"
            };

            for (String selector : captchaSelectors) {
                if (page.locator(selector).count() > 0) {
                    log.debug("CAPTCHA detected with selector: {}", selector);
                    return true;
                }
            }

            // Check for CAPTCHA-related text
            String pageText = page.textContent("body").toLowerCase();
            if (pageText.contains("captcha") || pageText.contains("human verification") || 
                pageText.contains("prove you're human") || pageText.contains("i'm not a robot")) {
                log.debug("CAPTCHA detected from page text");
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Error detecting CAPTCHA", e);
            return false;
        }
    }

    /**
     * Determine if application can be auto-submitted based on confidence scores
     */
    private boolean determineAutoSubmitEligibility(QuestionProcessingResult questionResult) {
        // Rule 1: No questions with confidence below 50%
        boolean hasLowConfidence = questionResult.getProcessedQuestions().stream()
                .anyMatch(q -> q.getConfidenceScore() < 0.5);

        if (hasLowConfidence) {
            log.info("Cannot auto-submit: found questions with confidence below 50%");
            return false;
        }

        // Rule 2: Questions with confidence 50-74% require user confirmation
        boolean hasMediumConfidence = questionResult.getProcessedQuestions().stream()
                .anyMatch(q -> q.getConfidenceScore() < 0.75);

        if (hasMediumConfidence) {
            log.info("Cannot auto-submit: found questions with confidence between 50-74% requiring user confirmation");
            return false;
        }

        // Rule 3: Overall confidence should be above 75%
        if (questionResult.getOverallConfidenceScore() < 0.75) {
            log.info("Cannot auto-submit: overall confidence score below 75%");
            return false;
        }

        // Rule 4: All required fields must be filled
        boolean allRequiredFilled = questionResult.getProcessedQuestions().stream()
                .allMatch(QuestionAnswerPair::isSuccessfullyFilled);

        if (!allRequiredFilled) {
            log.info("Cannot auto-submit: not all required fields were successfully filled");
            return false;
        }

        log.info("Application eligible for auto-submission");
        return true;
    }

    /**
     * Submit application form
     */
    private boolean submitApplication(Page page, String platform) {
        try {
            log.info("Submitting application for platform: {}", platform);

            // Platform-specific submit logic
            switch (platform.toLowerCase()) {
                case "naukri":
                    return submitNaukriApplication(page);
                case "linkedin":
                    return submitLinkedInApplication(page);
                case "indeed":
                    return submitIndeedApplication(page);
                default:
                    return submitGenericApplication(page);
            }

        } catch (Exception e) {
            log.error("Error submitting application", e);
            return false;
        }
    }

    /**
     * Submit Naukri application
     */
    private boolean submitNaukriApplication(Page page) {
        try {
            // Try common Naukri submit button selectors
            String[] submitSelectors = {
                "button[type='submit']",
                "input[type='submit']",
                "button:has-text('Apply')",
                "button:has-text('Submit')",
                ".apply-btn",
                "#apply-btn"
            };

            for (String selector : submitSelectors) {
                if (page.locator(selector).count() > 0) {
                    page.locator(selector).first().click();
                    page.waitForTimeout(2000);
                    return true;
                }
            }

            log.warn("Could not find Naukri submit button");
            return false;

        } catch (Exception e) {
            log.error("Error submitting Naukri application", e);
            return false;
        }
    }

    /**
     * Submit LinkedIn application
     */
    private boolean submitLinkedInApplication(Page page) {
        try {
            // LinkedIn Easy Apply typically has a specific button
            String[] submitSelectors = {
                "button[aria-label*='Submit']",
                "button:has-text('Submit application')",
                ".artdeco-button--primary:has-text('Submit')",
                "button[type='submit']"
            };

            for (String selector : submitSelectors) {
                if (page.locator(selector).count() > 0) {
                    page.locator(selector).first().click();
                    page.waitForTimeout(2000);
                    return true;
                }
            }

            log.warn("Could not find LinkedIn submit button");
            return false;

        } catch (Exception e) {
            log.error("Error submitting LinkedIn application", e);
            return false;
        }
    }

    /**
     * Submit Indeed application
     */
    private boolean submitIndeedApplication(Page page) {
        try {
            String[] submitSelectors = {
                "button[type='submit']",
                "input[type='submit']",
                "button:has-text('Apply')",
                "button:has-text('Submit')",
                ".apply-button"
            };

            for (String selector : submitSelectors) {
                if (page.locator(selector).count() > 0) {
                    page.locator(selector).first().click();
                    page.waitForTimeout(2000);
                    return true;
                }
            }

            log.warn("Could not find Indeed submit button");
            return false;

        } catch (Exception e) {
            log.error("Error submitting Indeed application", e);
            return false;
        }
    }

    /**
     * Submit generic application
     */
    private boolean submitGenericApplication(Page page) {
        try {
            // Try to find any submit button
            Locator submitButtons = page.locator("button[type='submit'], input[type='submit']");
            if (submitButtons.count() > 0) {
                submitButtons.first().click();
                page.waitForTimeout(2000);
                return true;
            }

            log.warn("Could not find generic submit button");
            return false;

        } catch (Exception e) {
            log.error("Error submitting generic application", e);
            return false;
        }
    }

    /**
     * Verify that application was successfully submitted
     */
    private boolean verifySubmission(Page page, String platform) {
        try {
            page.waitForTimeout(3000); // Wait for submission to process

            // Check for success indicators
            String[] successIndicators = {
                "Application submitted",
                "Successfully applied",
                "Thank you for applying",
                "Application received",
                "Your application has been submitted"
            };

            String pageText = page.textContent("body").toLowerCase();
            
            for (String indicator : successIndicators) {
                if (pageText.contains(indicator.toLowerCase())) {
                    log.info("Submission verified with indicator: {}", indicator);
                    return true;
                }
            }

            // Check for redirect to success page
            String currentUrl = page.url();
            if (currentUrl.contains("success") || currentUrl.contains("thank") || currentUrl.contains("applied")) {
                log.info("Submission verified via URL redirect: {}", currentUrl);
                return true;
            }

            // Check for absence of form
            if (page.locator("form").count() == 0) {
                log.info("Submission verified - form no longer present");
                return true;
            }

            log.warn("Could not verify submission - no success indicators found");
            return false;

        } catch (Exception e) {
            log.error("Error verifying submission", e);
            return false;
        }
    }

    /**
     * Create application record in backend
     */
    private void createBackendApplicationRecord(Long userId, String platform, String jobUrl, Map<String, Object> jobData) {
        try {
            Map<String, Object> applicationData = new java.util.HashMap<>();
            applicationData.put("platform", platform);
            applicationData.put("jobUrl", jobUrl);
            applicationData.put("jobTitle", jobData.get("jobTitle"));
            applicationData.put("company", jobData.get("company"));
            applicationData.put("matchScore", jobData.get("matchScore"));
            applicationData.put("status", "APPLIED");
            applicationData.put("appliedAt", LocalDateTime.now().toString());

            Long applicationId = backendApiClient.createApplication(userId, applicationData);
            log.info("Created backend application record with ID: {}", applicationId);

        } catch (Exception e) {
            log.error("Error creating backend application record", e);
        }
    }

    /**
     * Save application history
     */
    @Transactional
    private void saveApplicationHistory(ApplicationResult result) {
        try {
            ApplicationHistory history = new ApplicationHistory();
            history.setUserId(result.getUserId());
            history.setPlatform(result.getPlatform());
            history.setJobUrl(result.getJobUrl());
            history.setApplicationStatus(result.getApplicationStatus());
            history.setSubmissionStatus(result.getSubmissionStatus());
            history.setFailureReason(result.getFailureReason());
            history.setAppliedAt(result.getEndTime());
            history.setCreatedAt(LocalDateTime.now());

            applicationHistoryRepository.save(history);
            log.info("Saved application history for user {} on platform {}", result.getUserId(), result.getPlatform());

        } catch (Exception e) {
            log.error("Error saving application history", e);
        }
    }

    /**
     * Result class for application processing
     */
    public static class ApplicationResult {
        private Long userId;
        private String platform;
        private String jobUrl;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean success;
        private String applicationStatus;
        private String submissionStatus;
        private boolean submissionVerified;
        private boolean canAutoSubmit;
        private String failureReason;
        private QuestionProcessingResult questionProcessingResult;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getJobUrl() { return jobUrl; }
        public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getApplicationStatus() { return applicationStatus; }
        public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
        public String getSubmissionStatus() { return submissionStatus; }
        public void setSubmissionStatus(String submissionStatus) { this.submissionStatus = submissionStatus; }
        public boolean isSubmissionVerified() { return submissionVerified; }
        public void setSubmissionVerified(boolean submissionVerified) { this.submissionVerified = submissionVerified; }
        public boolean isCanAutoSubmit() { return canAutoSubmit; }
        public void setCanAutoSubmit(boolean canAutoSubmit) { this.canAutoSubmit = canAutoSubmit; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public QuestionProcessingResult getQuestionProcessingResult() { return questionProcessingResult; }
        public void setQuestionProcessingResult(QuestionProcessingResult questionProcessingResult) { this.questionProcessingResult = questionProcessingResult; }
    }
}