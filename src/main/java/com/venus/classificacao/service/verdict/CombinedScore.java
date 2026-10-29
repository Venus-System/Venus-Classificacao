package com.venus.classificacao.service.verdict;

import static com.venus.classificacao.service.quality.ScoreScale.FULL_SCORE;

import com.venus.classificacao.service.quality.QualityResult;
import com.venus.classificacao.service.question.ProfileResult;
import java.util.OptionalDouble;

public record CombinedScore(double qualityPoints, OptionalDouble profilePoints, double total) {

    private static final double QUALITY_MAX_POINTS = 35;
    private static final double PROFILE_MAX_POINTS = 65;

    public static CombinedScore of(QualityResult qualityResult, ProfileResult profileResult) {
        double qualityScore = qualityResult.qualityScore();
        double qualityPoints = QUALITY_MAX_POINTS * qualityScore / FULL_SCORE;

        OptionalDouble compatibilityRatio = profileResult.compatibilityRatio();
        if (compatibilityRatio.isEmpty()) {
            return withoutProfileQuestions(qualityPoints, qualityScore);
        }

        double profilePoints = PROFILE_MAX_POINTS * compatibilityRatio.getAsDouble();
        double total = qualityPoints + profilePoints;

        return new CombinedScore(qualityPoints, OptionalDouble.of(profilePoints), total);
    }

    private static CombinedScore withoutProfileQuestions(double qualityPoints, double qualityScore) {
        return new CombinedScore(qualityPoints, OptionalDouble.empty(), qualityScore);
    }
}
