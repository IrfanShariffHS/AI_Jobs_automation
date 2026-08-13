package com.automation.repository;

import com.automation.model.ApprovedAnswerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovedAnswerHistoryRepository extends JpaRepository<ApprovedAnswerHistory, Long> {

    List<ApprovedAnswerHistory> findByUserIdAndQuestionType(Long userId, String questionType);

    @Query("SELECT a FROM ApprovedAnswerHistory a WHERE a.userId = :userId AND a.questionType = :questionType ORDER BY a.successRate DESC, a.usageCount DESC")
    List<ApprovedAnswerHistory> findBestAnswersByType(@Param("userId") Long userId, @Param("questionType") String questionType);

    @Query("SELECT a FROM ApprovedAnswerHistory a WHERE a.userId = :userId AND a.questionType = :questionType AND a.successRate >= :minSuccessRate ORDER BY a.usageCount DESC")
    List<ApprovedAnswerHistory> findReliableAnswersByType(@Param("userId") Long userId, @Param("questionType") String questionType, @Param("minSuccessRate") double minSuccessRate);

    Optional<ApprovedAnswerHistory> findFirstByUserIdAndQuestionTypeOrderByUsageCountDesc(Long userId, String questionType);
}