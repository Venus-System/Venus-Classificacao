package com.venus.classificacao.repository.scoring;

import com.venus.classificacao.entity.scoring.ProductScore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductScoreRepository extends JpaRepository<ProductScore, Long> {

    Optional<ProductScore> findByProductVersionIdAndScoringModelId(Long productVersionId, Long scoringModelId);
}
