package com.venus.classificacao.service.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BucketWeightsTest {

    private final BucketWeights baseWeights = new BucketWeights(Map.of(
            ScoreBucket.HEALTH, 35.0,
            ScoreBucket.PERFORMANCE, 20.0,
            ScoreBucket.ENVIRONMENTAL, 15.0,
            ScoreBucket.ETHICAL, 15.0));

    @Test
    void bucketWithoutScoreLeavesTheAverage() {
        double qualityScore = baseWeights.weightedAverage(Map.of(ScoreBucket.HEALTH, 80.0, ScoreBucket.ETHICAL, 50.0));

        assertThat(qualityScore).isCloseTo(71.0, within(1e-9));
    }

    @Test
    void noScoredBucketGivesZero() {
        assertThat(baseWeights.weightedAverage(Map.of())).isZero();
    }
}
