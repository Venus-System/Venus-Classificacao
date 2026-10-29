package com.venus.classificacao.service;

import static com.venus.classificacao.service.quality.ScoreScale.FULL_SCORE;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import com.venus.classificacao.service.explanation.Explanation;
import com.venus.classificacao.service.explanation.Reason;
import com.venus.classificacao.service.quality.QualityResult;
import com.venus.classificacao.service.quality.QualityScores;
import com.venus.classificacao.service.question.ProfileResult;
import com.venus.classificacao.service.verdict.CombinedScore;
import com.venus.classificacao.service.verdict.Evaluation;
import com.venus.classificacao.service.verdict.Verdict;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.OptionalDouble;

public record ClassificationResult(
        Long userId,
        Long productVersionId,
        Long scoringModelId,
        int finalScore,
        RecommendationLevel recommendationLevel,
        RiskLevel riskLevel,
        BigDecimal compatibilityPercentage,
        Integer ingredientCount,
        Integer unevaluatedIngredientCount,
        Breakdown breakdown,
        List<Reason> reasons,
        String summary,
        OffsetDateTime calculatedAt,
        int processingTimeMs
) {

    private static final int PERCENTAGE_DECIMALS = 2;
    private static final int POINTS_DECIMALS = 1;

    public ClassificationResult {
        reasons = List.copyOf(reasons);
    }

    public static ClassificationResult of(ClassificationTarget target, Evaluation evaluation, Explanation explanation,
            int processingTimeMs) {
        Verdict verdict = evaluation.verdict();
        BigDecimal compatibilityPercentage = compatibilityPercentage(evaluation.profileResult());
        Breakdown breakdown = Breakdown.of(evaluation.qualityResult(), evaluation.combinedScore());

        return new ClassificationResult(
                target.userId(),
                target.productVersionId(),
                target.scoringModelId(),
                verdict.finalScore(),
                verdict.recommendationLevel(),
                verdict.riskLevel(),
                compatibilityPercentage,
                evaluation.ingredientCount(),
                evaluation.unevaluatedIngredientCount(),
                breakdown,
                explanation.reasons(),
                explanation.summary(),
                OffsetDateTime.now(),
                processingTimeMs);
    }

    private static BigDecimal compatibilityPercentage(ProfileResult profileResult) {
        OptionalDouble compatibilityRatio = profileResult.compatibilityRatio();
        if (compatibilityRatio.isEmpty()) {
            return null;
        }

        double percentage = compatibilityRatio.getAsDouble() * FULL_SCORE;
        return BigDecimal.valueOf(percentage).setScale(PERCENTAGE_DECIMALS, RoundingMode.HALF_UP);
    }

    public record Breakdown(
            Integer qualityScore,
            BigDecimal qualityPoints,
            BigDecimal profilePoints,
            Integer healthScore,
            Integer environmentalScore,
            Integer ethicalScore,
            Integer performanceScore
    ) {

        static Breakdown of(QualityResult qualityResult, CombinedScore combinedScore) {
            QualityScores scores = QualityScores.of(qualityResult);
            OptionalDouble profilePoints = combinedScore.profilePoints();

            return new Breakdown(
                    scores.qualityScore(),
                    roundedPoints(combinedScore.qualityPoints()),
                    profilePoints.isPresent() ? roundedPoints(profilePoints.getAsDouble()) : null,
                    scores.healthScore(),
                    scores.environmentalScore(),
                    scores.ethicalScore(),
                    scores.performanceScore());
        }

        private static BigDecimal roundedPoints(double points) {
            return BigDecimal.valueOf(points).setScale(POINTS_DECIMALS, RoundingMode.HALF_UP);
        }
    }
}
