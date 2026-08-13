package com.automation.service;

import com.automation.dto.*;
import com.automation.model.*;
import com.automation.repository.*;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Comprehensive Application Question Answering Engine
 * Handles dynamic question detection, classification, answer generation, confidence scoring,
 * and form filling for all job application platforms
 */
@Service
public class QuestionAnswerEngine {

    private static final Logger log = LoggerFactory.getLogger(QuestionAnswerEngine.class);

    @Autowired
    private ApplicationQuestionHistoryRepository questionHistoryRepository;

    @Autowired
    private SemanticQuestionMappingRepository semanticMappingRepository;

    @Autowired
    private ApprovedAnswerHistoryRepository approvedAnswerRepository;

    @Autowired
    private BackendApiClient backendApiClient;

    // Question type patterns for classification
    private static final Map<Pattern, ApplicationQuestionHistory.QuestionType> QUESTION_PATTERNS = new LinkedHashMap<>();

    static {
        // Notice period and joining date questions
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(notice|serving|period).*"), ApplicationQuestionHistory.QuestionType.NOTICE_PERIOD);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(immediate|join|start.*date|available).*"), ApplicationQuestionHistory.QuestionType.JOINING_DATE);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(how.*soon|when.*can.*you).*"), ApplicationQuestionHistory.QuestionType.JOINING_DATE);

        // Salary/CTC questions
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(current|present).*(salary|ctc|package|compensation).*"), ApplicationQuestionHistory.QuestionType.CURRENT_CTC);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(expected|desired|looking).*(salary|ctc|package|compensation).*"), ApplicationQuestionHistory.QuestionType.EXPECTED_CTC);

        // Experience questions
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(total|overall|years).*(experience|exp).*"), ApplicationQuestionHistory.QuestionType.TOTAL_EXPERIENCE);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(relevant|specific).*(experience|exp).*"), ApplicationQuestionHistory.QuestionType.SKILL_EXPERIENCE);

        // Education questions
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(highest|qualification|degree|education|graduation).*"), ApplicationQuestionHistory.QuestionType.EDUCATION);

        // Location and relocation
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(current|preferred|location|city|based).*"), ApplicationQuestionHistory.QuestionType.LOCATION);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(relocate|willing.*shift|willing.*relocate|relocation).*"), ApplicationQuestionHistory.QuestionType.RELOCATION);

        // Work authorization and visa
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(work.*authorization|authorized|visa|permit|right.*to.*work).*"), ApplicationQuestionHistory.QuestionType.WORK_AUTHORIZATION);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(visa.*status|sponsorship).*"), ApplicationQuestionHistory.QuestionType.VISA_STATUS);

        // Contact information
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(phone|mobile|contact).*number"), ApplicationQuestionHistory.QuestionType.PHONE);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(email|mail).*address"), ApplicationQuestionHistory.QuestionType.EMAIL);

        // Yes/No questions
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(yes|no|true|false)$"), ApplicationQuestionHistory.QuestionType.YES_NO);
        QUESTION_PATTERNS.put(Pattern.compile("(?i).*(are.*you|do.*you|have.*you|is.*your).*"), ApplicationQuestionHistory.QuestionType.YES_NO);
    }

    /**
     * Process application form and answer all questions
     */
    @Transactional
    public QuestionProcessingResult processApplicationForm(Page page, String platform, String jobUrl, Long userId) {
        log.info("Processing application form for user {} on platform {}", userId, platform);

        QuestionProcessingResult result = new QuestionProcessingResult();
        result.setUserId(userId);
        result.setPlatform(platform);
        result.setJobUrl(jobUrl);
        result.setProcessedQuestions(new ArrayList<>());
        result.setSkippedQuestions(new ArrayList<>());
        result.setErrors(new ArrayList<>());

        try {
            // Get user profile data from backend
            Map<String, Object> userProfile = backendApiClient.getUserProfile(userId);
            Map<String, Object> activeResume = backendApiClient.getActiveResume(userId);

            // Detect and process all form fields
            List<FormField> formFields = detectFormFields(page);
            log.info("Detected {} form fields on page", formFields.size());

            for (FormField field : formFields) {
                try {
                    QuestionAnswerPair questionAnswer = processQuestion(field, userProfile, activeResume, jobUrl, userId, platform);
                    result.getProcessedQuestions().add(questionAnswer);

                    // Fill the form field
                    if (questionAnswer.getConfidenceScore() >= 0.75) {
                        boolean filled = fillFormField(page, field, questionAnswer.getAnswer());
                        questionAnswer.setSuccessfullyFilled(filled);

                        if (filled) {
                            log.info("Successfully filled field: {} with answer: {}", field.getQuestionText(), questionAnswer.getAnswer());
                        } else {
                            log.warn("Failed to fill field: {}", field.getQuestionText());
                            result.getErrors().add("Failed to fill field: " + field.getQuestionText());
                        }
                    } else {
                        log.info("Question requires user confirmation: {} (confidence: {})", field.getQuestionText(), questionAnswer.getConfidenceScore());
                        result.getSkippedQuestions().add(questionAnswer);
                    }

                } catch (Exception e) {
                    log.error("Error processing question: {}", field.getQuestionText(), e);
                    result.getErrors().add("Error processing question: " + field.getQuestionText() + " - " + e.getMessage());
                }
            }

            // Calculate overall confidence
            double avgConfidence = result.getProcessedQuestions().stream()
                    .mapToDouble(QuestionAnswerPair::getConfidenceScore)
                    .average()
                    .orElse(0.0);
            result.setOverallConfidenceScore(avgConfidence);

            log.info("Application form processing complete. Processed: {}, Skipped: {}, Errors: {}, Overall confidence: {}",
                    result.getProcessedQuestions().size(), result.getSkippedQuestions().size(), result.getErrors().size(), avgConfidence);

        } catch (Exception e) {
            log.error("Error processing application form", e);
            result.getErrors().add("General error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Detect all form fields on the page
     */
    private List<FormField> detectFormFields(Page page) {
        List<FormField> fields = new ArrayList<>();

        // Detect text inputs
        Locator textInputs = page.locator("input[type='text'], input[type='email'], input[type='tel'], input[type='number']");
        for (int i = 0; i < textInputs.count(); i++) {
            Locator input = textInputs.nth(i);
            if (input.isVisible()) {
                fields.add(extractFieldInfo(input, ApplicationQuestionHistory.FieldType.TEXT_INPUT));
            }
        }

        // Detect textareas
        Locator textareas = page.locator("textarea");
        for (int i = 0; i < textareas.count(); i++) {
            Locator textarea = textareas.nth(i);
            if (textarea.isVisible()) {
                fields.add(extractFieldInfo(textarea, ApplicationQuestionHistory.FieldType.TEXTAREA));
            }
        }

        // Detect select dropdowns
        Locator selects = page.locator("select");
        for (int i = 0; i < selects.count(); i++) {
            Locator select = selects.nth(i);
            if (select.isVisible()) {
                fields.add(extractFieldInfo(select, ApplicationQuestionHistory.FieldType.DROPDOWN));
            }
        }

        // Detect radio buttons
        Locator radios = page.locator("input[type='radio']");
        for (int i = 0; i < radios.count(); i++) {
            Locator radio = radios.nth(i);
            if (radio.isVisible()) {
                fields.add(extractFieldInfo(radio, ApplicationQuestionHistory.FieldType.RADIO_BUTTON));
            }
        }

        // Detect checkboxes
        Locator checkboxes = page.locator("input[type='checkbox']");
        for (int i = 0; i < checkboxes.count(); i++) {
            Locator checkbox = checkboxes.nth(i);
            if (checkbox.isVisible()) {
                fields.add(extractFieldInfo(checkbox, ApplicationQuestionHistory.FieldType.CHECKBOX));
            }
        }

        // Detect date fields
        Locator dateFields = page.locator("input[type='date']");
        for (int i = 0; i < dateFields.count(); i++) {
            Locator dateField = dateFields.nth(i);
            if (dateField.isVisible()) {
                fields.add(extractFieldInfo(dateField, ApplicationQuestionHistory.FieldType.DATE_FIELD));
            }
        }

        return fields;
    }

    /**
     * Extract field information from a form element
     */
    private FormField extractFieldInfo(Locator element, ApplicationQuestionHistory.FieldType fieldType) {
        FormField field = new FormField();
        field.setFieldType(fieldType);

        // Extract field identifier
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        field.setFieldIdentifier(id != null ? id : name);

        // Extract question text from label, placeholder, or nearby text
        String labelText = extractLabelText(element);
        String placeholder = element.getAttribute("placeholder");
        field.setQuestionText(labelText != null && !labelText.isEmpty() ? labelText : placeholder);

        // Check if required
        String required = element.getAttribute("required");
        field.setRequired(required != null && (required.equals("required") || required.equals("true")));

        return field;
    }

    /**
     * Extract label text for a form element
     */
    private String extractLabelText(Locator element) {
        try {
            // Try to find associated label
            String id = element.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                Locator label = element.page().locator(String.format("label[for='%s']", id));
                if (label.count() > 0) {
                    return label.first().textContent().trim();
                }
            }

            // Try to find preceding text
            Locator parent = element.locator("..");
            if (parent.count() > 0) {
                String parentText = parent.first().textContent();
                if (parentText != null) {
                    String elementText = element.textContent();
                    if (elementText != null) {
                        return parentText.replace(elementText, "").trim();
                    }
                    return parentText.trim();
                }
            }

        } catch (Exception e) {
            // Ignore extraction errors
        }
        return "";
    }

    /**
     * Process a single question and generate answer
     */
    @Transactional
    private QuestionAnswerPair processQuestion(FormField field, Map<String, Object> userProfile, 
                                              Map<String, Object> activeResume, String jobUrl, 
                                              Long userId, String platform) {
        String questionText = field.getQuestionText();
        String normalizedQuestion = normalizeQuestion(questionText);
        ApplicationQuestionHistory.QuestionType questionType = classifyQuestion(questionText);

        log.debug("Processing question: type={}, text='{}'", questionType, questionText);

        // Generate answer with confidence scoring
        AnswerGenerationResult answerResult = generateAnswer(questionType, questionText, userProfile, activeResume, userId);

        // Store question history
        ApplicationQuestionHistory history = new ApplicationQuestionHistory();
        history.setUserId(userId);
        history.setPlatform(platform);
        history.setJobUrl(jobUrl);
        history.setQuestionText(questionText);
        history.setNormalizedQuestion(normalizedQuestion);
        history.setQuestionType(questionType.name());
        history.setAnswer(answerResult.getAnswer());
        history.setAnswerSource(answerResult.getSource().name());
        history.setConfidenceScore(BigDecimal.valueOf(answerResult.getConfidence()));
        history.setFieldType(field.getFieldType().name());
        history.setFieldIdentifier(field.getFieldIdentifier());
        history.setIsRequired(field.isRequired());
        history.setSuccessfullyFilled(false); // Will be updated after filling
        questionHistoryRepository.save(history);

        // Update semantic mappings
        updateSemanticMappings(userId, questionType, normalizedQuestion, questionText);

        // Update approved answer history if confidence is high
        if (answerResult.getConfidence() >= 0.9) {
            updateApprovedAnswerHistory(userId, questionType, answerResult.getAnswer(), answerResult.getConfidence());
        }

        boolean requiresUserConfirmation = answerResult.getConfidence() < 0.75;
        return new QuestionAnswerPair(questionText, questionType.name(), answerResult.getAnswer(), 
                answerResult.getSource().name(), answerResult.getConfidence(), false, requiresUserConfirmation);
    }

    /**
     * Normalize question text for consistent matching
     */
    String normalizeQuestion(String questionText) {
        if (questionText == null) return "";
        return questionText.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }

    /**
     * Classify question type using pattern matching
     */
    ApplicationQuestionHistory.QuestionType classifyQuestion(String questionText) {
        if (questionText == null || questionText.trim().isEmpty()) {
            return ApplicationQuestionHistory.QuestionType.UNKNOWN;
        }

        for (Map.Entry<Pattern, ApplicationQuestionHistory.QuestionType> entry : QUESTION_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(questionText).matches()) {
                return entry.getValue();
            }
        }

        return ApplicationQuestionHistory.QuestionType.UNKNOWN;
    }

    /**
     * Generate answer for a question with confidence scoring
     */
    AnswerGenerationResult generateAnswer(ApplicationQuestionHistory.QuestionType questionType, 
                                                String questionText,
                                                Map<String, Object> userProfile,
                                                Map<String, Object> activeResume,
                                                Long userId) {
        // Priority 1: User profile/settings
        AnswerGenerationResult profileResult = getAnswerFromProfile(questionType, userProfile);
        if (profileResult != null && profileResult.getConfidence() >= 0.9) {
            return profileResult;
        }

        // Priority 2: Resume data
        AnswerGenerationResult resumeResult = getAnswerFromResume(questionType, questionText, activeResume);
        if (resumeResult != null && resumeResult.getConfidence() >= 0.85) {
            return resumeResult;
        }

        // Priority 3: Previously approved answers
        AnswerGenerationResult approvedResult = getAnswerFromHistory(questionType, userId);
        if (approvedResult != null && approvedResult.getConfidence() >= 0.8) {
            return approvedResult;
        }

        // Priority 4: AI-generated answer
        AnswerGenerationResult aiResult = generateAIAnswer(questionType, questionText, userProfile, activeResume);
        if (aiResult != null) {
            return aiResult;
        }

        // Priority 5: Require user input
        return new AnswerGenerationResult("", ApplicationQuestionHistory.AnswerSource.USER_INPUT, 0.0);
    }

    /**
     * Get answer from user profile
     */
    AnswerGenerationResult getAnswerFromProfile(ApplicationQuestionHistory.QuestionType questionType, 
                                                        Map<String, Object> userProfile) {
        if (userProfile == null) return null;

        String answer = null;
        double confidence = 0.0;

        switch (questionType) {
            case PHONE:
                answer = (String) userProfile.get("phoneNumber");
                confidence = 0.95;
                break;
            case EMAIL:
                answer = (String) userProfile.get("email");
                confidence = 0.95;
                break;
            case LOCATION:
                answer = (String) userProfile.get("preferredLocation");
                confidence = 0.9;
                break;
            case TOTAL_EXPERIENCE:
                Object experience = userProfile.get("experience");
                if (experience != null) {
                    answer = experience.toString();
                    confidence = 0.9;
                }
                break;
            default:
                return null;
        }

        if (answer != null && !answer.isEmpty()) {
            return new AnswerGenerationResult(answer, ApplicationQuestionHistory.AnswerSource.USER_PROFILE, confidence);
        }

        return null;
    }

    /**
     * Get answer from resume data
     */
    private AnswerGenerationResult getAnswerFromResume(ApplicationQuestionHistory.QuestionType questionType,
                                                      String questionText, Map<String, Object> activeResume) {
        if (activeResume == null) return null;

        String answer = null;
        double confidence = 0.0;

        switch (questionType) {
            case EDUCATION:
                Object education = activeResume.get("education");
                if (education != null) {
                    answer = education.toString();
                    confidence = 0.85;
                }
                break;
            case SKILL_EXPERIENCE:
                Object skills = activeResume.get("skills");
                if (skills != null) {
                    answer = skills.toString();
                    confidence = 0.85;
                }
                break;
            default:
                return null;
        }

        if (answer != null && !answer.isEmpty()) {
            return new AnswerGenerationResult(answer, ApplicationQuestionHistory.AnswerSource.RESUME_DATA, confidence);
        }

        return null;
    }

    /**
     * Get answer from previously approved answers
     */
    private AnswerGenerationResult getAnswerFromHistory(ApplicationQuestionHistory.QuestionType questionType, Long userId) {
        List<ApprovedAnswerHistory> approvedAnswers = approvedAnswerRepository.findBestAnswersByType(userId, questionType.name());

        if (!approvedAnswers.isEmpty()) {
            ApprovedAnswerHistory bestAnswer = approvedAnswers.get(0);
            double confidence = bestAnswer.getSuccessRate() * 0.9; // Scale success rate to confidence
            return new AnswerGenerationResult(bestAnswer.getAnswer(), ApplicationQuestionHistory.AnswerSource.PREVIOUSLY_APPROVED, confidence);
        }

        return null;
    }

    /**
     * Generate AI answer for subjective questions
     */
    private AnswerGenerationResult generateAIAnswer(ApplicationQuestionHistory.QuestionType questionType,
                                                    String questionText, Map<String, Object> userProfile,
                                                    Map<String, Object> activeResume) {
        // For now, return a placeholder - this would integrate with AI services
        // In a full implementation, this would call the backend's AI services
        
        if (questionType == ApplicationQuestionHistory.QuestionType.UNKNOWN) {
            return new AnswerGenerationResult("", ApplicationQuestionHistory.AnswerSource.USER_INPUT, 0.0);
        }

        // Generate contextual answer based on question type
        String answer = generateContextualAnswer(questionType, questionText, userProfile);
        double confidence = 0.7; // Moderate confidence for AI-generated answers

        return new AnswerGenerationResult(answer, ApplicationQuestionHistory.AnswerSource.AI_GENERATED, confidence);
    }

    /**
     * Generate contextual answer based on question type
     */
    String generateContextualAnswer(ApplicationQuestionHistory.QuestionType questionType, 
                                          String questionText, Map<String, Object> userProfile) {
        switch (questionType) {
            case NOTICE_PERIOD:
                return "1 month";
            case JOINING_DATE:
                return "Within 1 month";
            case CURRENT_CTC:
                return "As per industry standards";
            case EXPECTED_CTC:
                return "As per company standards";
            case RELOCATION:
                return "Yes, willing to relocate";
            case WORK_AUTHORIZATION:
                return "Yes, legally authorized to work";
            case YES_NO:
                if (questionText.toLowerCase().contains("available") || 
                    questionText.toLowerCase().contains("willing")) {
                    return "Yes";
                }
                return "Yes";
            default:
                return "Please provide more information";
        }
    }

    /**
     * Fill form field with answer
     */
    private boolean fillFormField(Page page, FormField field, String answer) {
        try {
            Locator element = page.locator(buildFieldSelector(field));
            if (!element.isVisible()) {
                element.scrollIntoViewIfNeeded();
                page.waitForTimeout(200);
            }

            switch (field.getFieldType()) {
                case TEXT_INPUT:
                case TEXTAREA:
                    element.fill(answer);
                    break;
                case DROPDOWN:
                case SELECT:
                    element.selectOption(answer);
                    break;
                case RADIO_BUTTON:
                case CHECKBOX:
                    element.check();
                    break;
                case DATE_FIELD:
                    element.fill(answer);
                    break;
                default:
                    element.fill(answer);
            }

            // Verify the value was set
            String actualValue = element.inputValue();
            if (actualValue != null && actualValue.contains(answer)) {
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Error filling field: {}", field.getQuestionText(), e);
            return false;
        }
    }

    /**
     * Build selector for form field
     */
    private String buildFieldSelector(FormField field) {
        if (field.getFieldIdentifier() != null && !field.getFieldIdentifier().isEmpty()) {
            if (field.getFieldIdentifier().startsWith("#")) {
                return field.getFieldIdentifier();
            }
            return "#" + field.getFieldIdentifier();
        }
        return String.format("[name='%s']", field.getFieldIdentifier());
    }

    /**
     * Update semantic mappings for question
     */
    @Transactional
    void updateSemanticMappings(Long userId, ApplicationQuestionHistory.QuestionType questionType, 
                                       String normalizedQuestion, String originalQuestion) {
        Optional<SemanticQuestionMapping> existing = semanticMappingRepository
                .findByUserIdAndCanonicalQuestion(userId, normalizedQuestion);

        if (existing.isPresent()) {
            // Update variants if needed
            SemanticQuestionMapping mapping = existing.get();
            // Add new variant to JSON variants list (implementation depends on JSON handling)
            semanticMappingRepository.save(mapping);
        } else {
            // Create new mapping
            SemanticQuestionMapping newMapping = new SemanticQuestionMapping();
            newMapping.setUserId(userId);
            newMapping.setCanonicalQuestion(normalizedQuestion);
            newMapping.setQuestionType(questionType.name());
            newMapping.setVariants(String.format("[\"%s\"]", originalQuestion.replace("\"", "\\\"")));
            semanticMappingRepository.save(newMapping);
        }
    }

    /**
     * Update approved answer history
     */
    @Transactional
    void updateApprovedAnswerHistory(Long userId, ApplicationQuestionHistory.QuestionType questionType, 
                                           String answer, double confidence) {
        Optional<ApprovedAnswerHistory> existing = approvedAnswerRepository
                .findFirstByUserIdAndQuestionTypeOrderByUsageCountDesc(userId, questionType.name());

        if (existing.isPresent()) {
            ApprovedAnswerHistory history = existing.get();
            if (history.getAnswer().equals(answer)) {
                history.incrementUsage();
                approvedAnswerRepository.save(history);
            }
        } else {
            ApprovedAnswerHistory newHistory = new ApprovedAnswerHistory();
            newHistory.setUserId(userId);
            newHistory.setQuestionType(questionType.name());
            newHistory.setAnswer(answer);
            newHistory.setConfidenceScore(BigDecimal.valueOf(confidence));
            newHistory.setUsageCount(1);
            approvedAnswerRepository.save(newHistory);
        }
    }

    // Inner classes for data transfer

    public static class FormField {
        private String questionText;
        private ApplicationQuestionHistory.FieldType fieldType;
        private String fieldIdentifier;
        private boolean required;

        // Getters and Setters
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public ApplicationQuestionHistory.FieldType getFieldType() { return fieldType; }
        public void setFieldType(ApplicationQuestionHistory.FieldType fieldType) { this.fieldType = fieldType; }
        public String getFieldIdentifier() { return fieldIdentifier; }
        public void setFieldIdentifier(String fieldIdentifier) { this.fieldIdentifier = fieldIdentifier; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }

    public static class AnswerGenerationResult {
        private String answer;
        private ApplicationQuestionHistory.AnswerSource source;
        private double confidence;

        public AnswerGenerationResult(String answer, ApplicationQuestionHistory.AnswerSource source, double confidence) {
            this.answer = answer;
            this.source = source;
            this.confidence = confidence;
        }

        // Getters
        public String getAnswer() { return answer; }
        public ApplicationQuestionHistory.AnswerSource getSource() { return source; }
        public double getConfidence() { return confidence; }
    }
}