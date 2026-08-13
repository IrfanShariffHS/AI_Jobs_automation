package com.automation.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO for question-answer pairs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswerPair {
    private String questionText;
    private String questionType;
    private String answer;
    private String answerSource;
    private double confidenceScore;
    private boolean successfullyFilled;
    private boolean requiresUserConfirmation;
}