package com.venus.classificacao.service.quality;

import java.util.OptionalDouble;

public record QualityScores(
        int qualityScore,
        Integer healthScore,
        Integer environmentalScore,
        Integer ethicalScore,
        Integer performanceScore
) {

    public static QualityScores of(QualityResult qualityResult) {
        return new QualityScores(
                (int) Math.round(qualityResult.qualityScore()),
                roundedScore(qualityResult.scoreOf(ScoreBucket.HEALTH)),
                roundedScore(qualityResult.scoreOf(ScoreBucket.ENVIRONMENTAL)),
                roundedScore(qualityResult.scoreOf(ScoreBucket.ETHICAL)),
                roundedScore(qualityResult.scoreOf(ScoreBucket.PERFORMANCE)));
    }

    private static Integer roundedScore(OptionalDouble score) {
        return score.isPresent() ? (int) Math.round(score.getAsDouble()) : null;
    }
}
