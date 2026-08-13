package com.automation.repository;

import com.automation.model.ApplicationQuestionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationQuestionHistoryRepository extends JpaRepository<ApplicationQuestionHistory, Long> {

    List<ApplicationQuestionHistory> findByUserIdAndPlatform(Long userId, String platform);

    List<ApplicationQuestionHistory> findByUserIdAndQuestionType(Long userId, String questionType);

    @Query("SELECT q FROM ApplicationQuestionHistory q WHERE q.userId = :userId AND q.normalizedQuestion = :normalized")
    Optional<ApplicationQuestionHistory> findByUserIdAndNormalizedQuestion(@Param("userId") Long userId, @Param("normalized") String normalized);

    @Query("SELECT q FROM ApplicationQuestionHistory q WHERE q.userId = :userId AND q.questionType = :questionType AND q.successfullyFilled = true ORDER BY q.createdAt DESC")
    List<ApplicationQuestionHistory> findSuccessfulAnswersByType(@Param("userId") Long userId, @Param("questionType") String questionType);

    @Query("SELECT COUNT(q) FROM ApplicationQuestionHistory q WHERE q.userId = :userId AND q.successfullyFilled = true")
    long countSuccessfulAnswersByUser(@Param("userId") Long userId);

    @Query("SELECT AVG(q.confidenceScore) FROM ApplicationQuestionHistory q WHERE q.userId = :userId AND q.successfullyFilled = true")
    Double getAverageConfidenceScore(@Param("userId") Long userId);
}