package com.venus.classificacao.repository.scoring;

import com.venus.classificacao.entity.scoring.ScoringModelCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoringModelCategoryRepository extends JpaRepository<ScoringModelCategory, Long> {

    List<ScoringModelCategory> findByScoringModelId(Long scoringModelId);
}
