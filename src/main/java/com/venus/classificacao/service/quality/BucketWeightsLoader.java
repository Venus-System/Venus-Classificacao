package com.venus.classificacao.service.quality;

import com.venus.classificacao.entity.scoring.ScoreCategory;
import com.venus.classificacao.entity.scoring.ScoringModelCategory;
import com.venus.classificacao.repository.scoring.ScoreCategoryRepository;
import com.venus.classificacao.repository.scoring.ScoringModelCategoryRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BucketWeightsLoader {

    private static final double NO_REINFORCEMENT = 1.0;

    private final ScoreCategoryRepository scoreCategoryRepository;
    private final ScoringModelCategoryRepository scoringModelCategoryRepository;

    public BucketWeightsLoader(ScoreCategoryRepository scoreCategoryRepository,
            ScoringModelCategoryRepository scoringModelCategoryRepository) {
        this.scoreCategoryRepository = scoreCategoryRepository;
        this.scoringModelCategoryRepository = scoringModelCategoryRepository;
    }

    public BucketWeights load(Long scoringModelId) {
        Map<Long, BigDecimal> modelWeightByCategoryId = modelWeightByCategoryId(scoringModelId);
        Map<ScoreBucket, Double> defaultWeightSumByBucket = new EnumMap<>(ScoreBucket.class);
        Map<ScoreBucket, Double> modelWeightSumByBucket = new EnumMap<>(ScoreBucket.class);

        for (ScoreCategory category : scoreCategoryRepository.findAll()) {
            ScoreBucket bucket = ScoreCategoryBuckets.bucketOf(category.getName());
            if (!bucket.isScored()) {
                continue;
            }

            BigDecimal defaultWeight = category.getDefaultWeight();
            BigDecimal modelWeight = modelWeightByCategoryId.getOrDefault(category.getId(), defaultWeight);

            defaultWeightSumByBucket.merge(bucket, defaultWeight.doubleValue(), Double::sum);
            modelWeightSumByBucket.merge(bucket, modelWeight.doubleValue(), Double::sum);
        }

        Map<ScoreBucket, Double> weightByBucket = weightByBucket(defaultWeightSumByBucket, modelWeightSumByBucket);
        return new BucketWeights(weightByBucket);
    }

    private Map<Long, BigDecimal> modelWeightByCategoryId(Long scoringModelId) {
        List<ScoringModelCategory> modelCategories = scoringModelCategoryRepository.findByScoringModelId(scoringModelId);

        return modelCategories.stream()
                .collect(Collectors.toMap(modelCategory -> modelCategory.getScoreCategory().getId(),
                        ScoringModelCategory::getWeight));
    }

    private Map<ScoreBucket, Double> weightByBucket(Map<ScoreBucket, Double> defaultWeightSumByBucket,
            Map<ScoreBucket, Double> modelWeightSumByBucket) {
        Map<ScoreBucket, Double> weightByBucket = new EnumMap<>(ScoreBucket.class);

        for (ScoreBucket bucket : ScoreBucket.values()) {
            if (!bucket.isScored()) {
                continue;
            }

            Double defaultWeightSum = defaultWeightSumByBucket.get(bucket);
            Double modelWeightSum = modelWeightSumByBucket.get(bucket);
            double reinforcement = reinforcementOf(defaultWeightSum, modelWeightSum);

            weightByBucket.put(bucket, bucket.baseWeight() * reinforcement);
        }

        return weightByBucket;
    }

    private double reinforcementOf(Double defaultWeightSum, Double modelWeightSum) {
        if (defaultWeightSum == null || defaultWeightSum == 0) {
            return NO_REINFORCEMENT;
        }

        return modelWeightSum / defaultWeightSum;
    }
}
