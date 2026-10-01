package com.venus.classificacao.repository.scoring;

import com.venus.classificacao.entity.scoring.ScoringModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoringModelRepository extends JpaRepository<ScoringModel, Long> {

    Optional<ScoringModel> findFirstByIsActiveTrueOrderByIdDesc();
}
