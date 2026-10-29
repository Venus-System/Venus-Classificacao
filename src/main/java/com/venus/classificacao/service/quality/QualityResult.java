package com.venus.classificacao.service.quality;

import java.util.Map;
import java.util.OptionalDouble;

public record QualityResult(Map<ScoreBucket, Double> scoreByBucket, double qualityScore) {

    public QualityResult {
        scoreByBucket = Map.copyOf(scoreByBucket);
    }

    public OptionalDouble scoreOf(ScoreBucket bucket) {
        Double score = scoreByBucket.get(bucket);
        return score == null ? OptionalDouble.empty() : OptionalDouble.of(score);
    }
}
