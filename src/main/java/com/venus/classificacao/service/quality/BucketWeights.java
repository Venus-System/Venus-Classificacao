package com.venus.classificacao.service.quality;

import java.util.Map;

public record BucketWeights(Map<ScoreBucket, Double> weightByBucket) {

    public BucketWeights {
        weightByBucket = Map.copyOf(weightByBucket);
    }

    public double weightedAverage(Map<ScoreBucket, Double> scoreByBucket) {
        double weightSum = scoreByBucket.keySet().stream()
                .mapToDouble(this::weightOf)
                .sum();

        if (weightSum == 0) {
            return 0;
        }

        double weightedScoreSum = scoreByBucket.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * weightOf(entry.getKey()))
                .sum();

        return weightedScoreSum / weightSum;
    }

    private double weightOf(ScoreBucket bucket) {
        return weightByBucket.getOrDefault(bucket, 0.0);
    }
}
