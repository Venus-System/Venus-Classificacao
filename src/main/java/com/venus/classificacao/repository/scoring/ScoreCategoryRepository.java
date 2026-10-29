package com.venus.classificacao.repository.scoring;

import com.venus.classificacao.entity.scoring.ScoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreCategoryRepository extends JpaRepository<ScoreCategory, Long> {
}
