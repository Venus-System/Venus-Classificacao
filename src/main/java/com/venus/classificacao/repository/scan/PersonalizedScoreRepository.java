package com.venus.classificacao.repository.scan;

import com.venus.classificacao.entity.scan.PersonalizedScore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalizedScoreRepository extends JpaRepository<PersonalizedScore, Long> {

    Optional<PersonalizedScore> findByAnalysisResultId(Long analysisResultId);
}
