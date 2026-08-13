package com.automation.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionProcessingResult {
    private Long userId;
    private String platform;
    private String jobUrl;
    private List<QuestionAnswerPair> processedQuestions;
    private List<QuestionAnswerPair> skippedQuestions;
    private List<String> errors;
    private double overallConfidenceScore;
    private boolean requiresUserConfirmation;
    private boolean canAutoSubmit;
}