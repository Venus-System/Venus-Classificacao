package com.venus.classificacao.repository.scan;

import com.venus.classificacao.entity.scan.RuleEvaluation;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleEvaluationRepository extends JpaRepository<RuleEvaluation, Long> {

    @EntityGraph(attributePaths = {"compatibilityRule", "ingredient", "profileTag"})
    List<RuleEvaluation> findByAnalysisResultId(Long analysisResultId);
}
