package com.automation.repository;

import com.automation.model.SemanticQuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemanticQuestionMappingRepository extends JpaRepository<SemanticQuestionMapping, Long> {

    List<SemanticQuestionMapping> findByUserIdAndQuestionType(Long userId, String questionType);

    Optional<SemanticQuestionMapping> findByUserIdAndCanonicalQuestion(Long userId, String canonicalQuestion);

    @Query("SELECT s FROM SemanticQuestionMapping s WHERE s.userId = :userId AND s.questionType = :questionType")
    List<SemanticQuestionMapping> findMappingsByUserAndType(@Param("userId") Long userId, @Param("questionType") String questionType);
}