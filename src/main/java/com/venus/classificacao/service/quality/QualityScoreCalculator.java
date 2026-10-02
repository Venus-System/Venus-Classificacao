package com.venus.classificacao.service.quality;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.strategy.BucketScoreStrategy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QualityScoreCalculator {

    private final List<BucketScoreStrategy> bucketStrategies;

    public QualityScoreCalculator(List<BucketScoreStrategy> bucketStrategies) {
        this.bucketStrategies = bucketStrategies;
    }

    public QualityResult calculate(ProductSnapshot product, BucketWeights bucketWeights) {
        Map<ScoreBucket, Double> scoreByBucket = new EnumMap<>(ScoreBucket.class);
        for (BucketScoreStrategy strategy : bucketStrategies) {
            strategy.score(product).ifPresent(score -> scoreByBucket.put(strategy.bucket(), score));
        }

        double qualityScore = bucketWeights.weightedAverage(scoreByBucket);
        return new QualityResult(scoreByBucket, qualityScore);
    }
}
