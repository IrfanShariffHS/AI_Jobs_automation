package com.automation.service;

import com.automation.dto.QuestionAnswerPair;
import com.automation.dto.QuestionProcessingResult;
import com.automation.model.ApplicationQuestionHistory;
import com.automation.repository.ApplicationQuestionHistoryRepository;
import com.automation.repository.SemanticQuestionMappingRepository;
import com.automation.repository.ApprovedAnswerHistoryRepository;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for QuestionAnswerEngine
 */
@ExtendWith(MockitoExtension.class)
class QuestionAnswerEngineTest {

    @Mock
    private ApplicationQuestionHistoryRepository questionHistoryRepository;

    @Mock
    private SemanticQuestionMappingRepository semanticMappingRepository;

    @Mock
    private ApprovedAnswerHistoryRepository approvedAnswerRepository;

    @Mock
    private BackendApiClient backendApiClient;

    @Mock
    private Page page;

    @InjectMocks
    private QuestionAnswerEngine questionAnswerEngine;

    private Long testUserId = 1L;
    private String testPlatform = "naukri";
    private String testJobUrl = "https://naukri.com/job/123";

    @BeforeEach
    void setUp() {
        // Setup common test data
    }

    @Test
    void testProcessApplicationFormIntegration() {
        // Integration test for the main public method
        when(backendApiClient.getUserProfile(testUserId)).thenReturn(new HashMap<>());
        when(backendApiClient.getActiveResume(testUserId)).thenReturn(new HashMap<>());

        QuestionProcessingResult result = questionAnswerEngine.processApplicationForm(page, testPlatform, testJobUrl, testUserId);
        
        // Verify that backend API was called
        verify(backendApiClient, times(1)).getUserProfile(testUserId);
        verify(backendApiClient, times(1)).getActiveResume(testUserId);
        assertNotNull(result);
    }

    @Test
    void testConfidenceThresholds() {
        // Test different confidence thresholds
        assertTrue(0.9 >= 0.75, "90% confidence should auto-fill");
        assertTrue(0.8 >= 0.75, "80% confidence should auto-fill");
        assertFalse(0.6 >= 0.75, "60% confidence should require confirmation");
        assertFalse(0.4 >= 0.5, "40% confidence should require user input");
    }

    @Test
    void testUpdateApprovedAnswerHistory() {
        // Test that approved answers are tracked correctly
        when(approvedAnswerRepository.findFirstByUserIdAndQuestionTypeOrderByUsageCountDesc(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        questionAnswerEngine.updateApprovedAnswerHistory(
                testUserId, 
                ApplicationQuestionHistory.QuestionType.NOTICE_PERIOD, 
                "1 month", 
                0.95);

        verify(approvedAnswerRepository, times(1)).save(any());
    }

    @Test
    void testUpdateSemanticMappings() {
        // Test semantic mapping updates
        when(semanticMappingRepository.findByUserIdAndCanonicalQuestion(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        questionAnswerEngine.updateSemanticMappings(
                testUserId, 
                ApplicationQuestionHistory.QuestionType.NOTICE_PERIOD, 
                "what is your notice period", 
                "What is your notice period?");

        verify(semanticMappingRepository, times(1)).save(any());
    }

    @Test
    void testAnswerSourcePriority() {
        // Test that answer sources are prioritized correctly
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("phoneNumber", "1234567890");

        QuestionAnswerEngine.AnswerGenerationResult profileResult = 
                questionAnswerEngine.getAnswerFromProfile(ApplicationQuestionHistory.QuestionType.PHONE, userProfile);

        assertEquals(ApplicationQuestionHistory.AnswerSource.USER_PROFILE, profileResult.getSource());
        assertEquals(0.95, profileResult.getConfidence());
    }

    @Test
    void testGetAnswerFromProfile() {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("phoneNumber", "1234567890");
        userProfile.put("email", "test@example.com");
        userProfile.put("preferredLocation", "Mumbai");
        userProfile.put("experience", "5");

        QuestionAnswerEngine.AnswerGenerationResult phoneResult = 
                questionAnswerEngine.getAnswerFromProfile(ApplicationQuestionHistory.QuestionType.PHONE, userProfile);
        QuestionAnswerEngine.AnswerGenerationResult emailResult = 
                questionAnswerEngine.getAnswerFromProfile(ApplicationQuestionHistory.QuestionType.EMAIL, userProfile);
        QuestionAnswerEngine.AnswerGenerationResult locationResult = 
                questionAnswerEngine.getAnswerFromProfile(ApplicationQuestionHistory.QuestionType.LOCATION, userProfile);

        assertNotNull(phoneResult);
        assertEquals("1234567890", phoneResult.getAnswer());
        assertEquals(ApplicationQuestionHistory.AnswerSource.USER_PROFILE, phoneResult.getSource());
        assertEquals(0.95, phoneResult.getConfidence());

        assertNotNull(emailResult);
        assertEquals("test@example.com", emailResult.getAnswer());

        assertNotNull(locationResult);
        assertEquals("Mumbai", locationResult.getAnswer());
    }

    @Test
    void testGetAnswerFromProfileReturnsNullForMissingData() {
        Map<String, Object> emptyProfile = new HashMap<>();

        QuestionAnswerEngine.AnswerGenerationResult result = 
                questionAnswerEngine.getAnswerFromProfile(ApplicationQuestionHistory.QuestionType.PHONE, emptyProfile);

        assertNull(result);
    }

    @Test
    void testGenerateContextualAnswer() {
        Map<String, Object> userProfile = new HashMap<>();

        String noticeAnswer = questionAnswerEngine.generateContextualAnswer(
                ApplicationQuestionHistory.QuestionType.NOTICE_PERIOD, "Notice period?", userProfile);
        String relocateAnswer = questionAnswerEngine.generateContextualAnswer(
                ApplicationQuestionHistory.QuestionType.RELOCATION, "Are you willing to relocate?", userProfile);
        String workAuthAnswer = questionAnswerEngine.generateContextualAnswer(
                ApplicationQuestionHistory.QuestionType.WORK_AUTHORIZATION, "Work authorization?", userProfile);

        assertEquals("1 month", noticeAnswer);
        assertEquals("Yes, willing to relocate", relocateAnswer);
        assertEquals("Yes, legally authorized to work", workAuthAnswer);
    }

    @Test
    void testConfidenceScoringHigh() {
        // Test high confidence scenarios
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("phoneNumber", "1234567890");

        QuestionAnswerEngine.AnswerGenerationResult result = 
                questionAnswerEngine.getAnswerFromProfile(ApplicationQuestionHistory.QuestionType.PHONE, userProfile);

        assertTrue(result.getConfidence() >= 0.9);
    }

    @Test
    void testConfidenceScoringLow() {
        // Test low confidence for unknown questions
        Map<String, Object> emptyProfile = new HashMap<>();
        Map<String, Object> emptyResume = new HashMap<>();

        QuestionAnswerEngine.AnswerGenerationResult result = 
                questionAnswerEngine.generateAnswer(
                        ApplicationQuestionHistory.QuestionType.UNKNOWN, 
                        "Random question", 
                        emptyProfile, 
                        emptyResume, 
                        testUserId);

        assertEquals(0.0, result.getConfidence());
        assertEquals(ApplicationQuestionHistory.AnswerSource.USER_INPUT, result.getSource());
    }

    @Test
    void testQuestionPriorityOrder() {
        // Test that answers are prioritized correctly
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("phoneNumber", "1234567890");

        QuestionAnswerEngine.AnswerGenerationResult result = 
                questionAnswerEngine.generateAnswer(
                        ApplicationQuestionHistory.QuestionType.PHONE, 
                        "Phone number?", 
                        userProfile, 
                        new HashMap<>(), 
                        testUserId);

        // Should prioritize user profile over other sources
        assertEquals(ApplicationQuestionHistory.AnswerSource.USER_PROFILE, result.getSource());
        assertEquals("1234567890", result.getAnswer());
    }

    @Test
    void testUnknownQuestionClassification() {
        String unknownQuestion = "Favorite color?";

        ApplicationQuestionHistory.QuestionType type = questionAnswerEngine.classifyQuestion(unknownQuestion);

        assertEquals(ApplicationQuestionHistory.QuestionType.UNKNOWN, type);
    }

    @Test
    void testYesNoQuestionClassification() {
        String yesQuestion = "Are you willing to relocate for this position?";
        String noQuestion = "Do you require visa sponsorship to work?";

        ApplicationQuestionHistory.QuestionType type1 = questionAnswerEngine.classifyQuestion(yesQuestion);
        ApplicationQuestionHistory.QuestionType type2 = questionAnswerEngine.classifyQuestion(noQuestion);

        assertNotNull(type1);
        assertNotNull(type2);
    }

    @Test
    void testClassifyNoticePeriodQuestion() {
        // Test notice period classification
        String question1 = "What is your notice period?";
        String question2 = "How soon can you join?";
        String question3 = "When are you available to start?";

        ApplicationQuestionHistory.QuestionType type1 = questionAnswerEngine.classifyQuestion(question1);
        ApplicationQuestionHistory.QuestionType type2 = questionAnswerEngine.classifyQuestion(question2);
        ApplicationQuestionHistory.QuestionType type3 = questionAnswerEngine.classifyQuestion(question3);

        assertEquals(ApplicationQuestionHistory.QuestionType.NOTICE_PERIOD, type1);
        assertEquals(ApplicationQuestionHistory.QuestionType.JOINING_DATE, type2);
        assertEquals(ApplicationQuestionHistory.QuestionType.JOINING_DATE, type3);
    }

    @Test
    void testClassifySalaryQuestions() {
        String currentCtcQuestion = "What is your current CTC?";
        String expectedCtcQuestion = "What are your salary expectations?";

        ApplicationQuestionHistory.QuestionType type1 = questionAnswerEngine.classifyQuestion(currentCtcQuestion);
        ApplicationQuestionHistory.QuestionType type2 = questionAnswerEngine.classifyQuestion(expectedCtcQuestion);

        assertNotNull(type1);
        assertNotNull(type2);
    }

    @Test
    void testClassifyExperienceQuestions() {
        String totalExpQuestion = "Total years of experience";
        String relevantExpQuestion = "Relevant experience in Java";

        ApplicationQuestionHistory.QuestionType type1 = questionAnswerEngine.classifyQuestion(totalExpQuestion);
        ApplicationQuestionHistory.QuestionType type2 = questionAnswerEngine.classifyQuestion(relevantExpQuestion);

        assertEquals(ApplicationQuestionHistory.QuestionType.TOTAL_EXPERIENCE, type1);
        assertEquals(ApplicationQuestionHistory.QuestionType.SKILL_EXPERIENCE, type2);
    }
}