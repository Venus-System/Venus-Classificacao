package com.venus.classificacao.service.quality;

import com.venus.classificacao.entity.scoring.ScoreCategory;
import com.venus.classificacao.entity.scoring.ScoringModelCategory;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.repository.scoring.ScoreCategoryRepository;
import com.venus.classificacao.repository.scoring.ScoringModelCategoryRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class BucketWeightsLoader {

    private static final Logger log = LoggerFactory.getLogger(BucketWeightsLoader.class);

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

        List<ScoreCategory> categories = executeOrFail(scoreCategoryRepository::findAll,
                "Falha ao consultar categorias de score no banco de dados");

        for (ScoreCategory category : categories) {
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
        List<ScoringModelCategory> modelCategories = executeOrFail(
                () -> scoringModelCategoryRepository.findByScoringModelId(scoringModelId),
                "Falha ao consultar categorias do modelo de scoring");

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

    private <T> T executeOrFail(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException ex) {
            throw DataIntegrityViolationTranslator.translate(ex);
        } catch (DataAccessException ex) {
            log.error(errorMessage, ex);
            throw DataAccessFailureTranslator.translate(ex, errorMessage);
        }
    }
}
