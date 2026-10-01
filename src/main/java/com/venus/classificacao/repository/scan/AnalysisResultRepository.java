package com.venus.classificacao.repository.scan;

import com.venus.classificacao.entity.scan.AnalysisResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Optional<AnalysisResult> findFirstByUserIdAndProductVersionIdAndScoringModelIdOrderByCreatedAtDescIdDesc(
            Long userId, Long productVersionId, Long scoringModelId);
}
