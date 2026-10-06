package com.venus.classificacao.service.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class QualityScoresTest {

    @Test
    void roundsTheScoresAndLeavesTheBucketWithoutDataNull() {
        QualityResult qualityResult = new QualityResult(
                Map.of(ScoreBucket.HEALTH, 80.5, ScoreBucket.ETHICAL, 50.0, ScoreBucket.PERFORMANCE, 66.4), 64.49);

        QualityScores scores = QualityScores.of(qualityResult);

        assertThat(scores).isEqualTo(new QualityScores(64, 81, null, 50, 66));
    }
}
