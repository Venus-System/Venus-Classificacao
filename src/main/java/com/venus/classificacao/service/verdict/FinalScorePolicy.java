package com.venus.classificacao.service.verdict;

import static com.venus.classificacao.service.quality.ScoreScale.FULL_SCORE;
import static com.venus.classificacao.service.quality.ScoreScale.MIN_SCORE;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FinalScorePolicy {

    private static final Set<RiskLevel> CONTRAINDICATING_SEVERITIES = EnumSet.of(RiskLevel.HIGH, RiskLevel.CRITICAL);

    private static final int HIGH_ALLERGY_MAX_SCORE = 15;
    private static final int MEDIUM_ALLERGY_MAX_SCORE = 49;
    private static final int LOW_ALLERGY_DISCOUNT = 15;

    private static final int IDEAL_FROM = 85;
    private static final int RECOMMENDED_FROM = 70;
    private static final int ACCEPTABLE_FROM = 50;
    private static final int NOT_RECOMMENDED_FROM = 25;

    public Verdict decide(double combinedScore, boolean blockedByRule, List<AllergyMatch> allergies) {
        Optional<RiskLevel> worstAllergy = worstSeverityOf(allergies);

        int roundedScore = roundToScoreRange(combinedScore);
        int finalScore = applyLocks(roundedScore, blockedByRule, worstAllergy);

        RecommendationLevel recommendationLevel = recommendationLevel(finalScore, blockedByRule, worstAllergy);
        RiskLevel riskLevel = riskLevel(finalScore, blockedByRule, worstAllergy);

        return new Verdict(finalScore, recommendationLevel, riskLevel);
    }

    private Optional<RiskLevel> worstSeverityOf(List<AllergyMatch> allergies) {
        return allergies.stream()
                .map(AllergyMatch::severity)
                .max(Comparator.naturalOrder());
    }

    private int roundToScoreRange(double score) {
        long roundedScore = Math.round(score);
        return (int) Math.max(MIN_SCORE, Math.min(FULL_SCORE, roundedScore));
    }

    private int applyLocks(int score, boolean blockedByRule, Optional<RiskLevel> worstAllergy) {
        if (blockedByRule) {
            return MIN_SCORE;
        }
        if (worstAllergy.isEmpty()) {
            return score;
        }

        return switch (worstAllergy.get()) {
            case CRITICAL -> MIN_SCORE;
            case HIGH -> Math.min(score, HIGH_ALLERGY_MAX_SCORE);
            case MEDIUM -> Math.min(score, MEDIUM_ALLERGY_MAX_SCORE);
            case LOW -> Math.max(MIN_SCORE, score - LOW_ALLERGY_DISCOUNT);
        };
    }

    private RecommendationLevel recommendationLevel(int finalScore, boolean blockedByRule,
            Optional<RiskLevel> worstAllergy) {
        if (isContraindicated(blockedByRule, worstAllergy)) {
            return RecommendationLevel.CONTRAINDICATED;
        }

        return recommendationByScore(finalScore);
    }

    private RecommendationLevel recommendationByScore(int finalScore) {
        if (finalScore >= IDEAL_FROM) {
            return RecommendationLevel.IDEAL;
        }
        if (finalScore >= RECOMMENDED_FROM) {
            return RecommendationLevel.RECOMMENDED;
        }
        if (finalScore >= ACCEPTABLE_FROM) {
            return RecommendationLevel.ACCEPTABLE;
        }
        if (finalScore >= NOT_RECOMMENDED_FROM) {
            return RecommendationLevel.NOT_RECOMMENDED;
        }
        return RecommendationLevel.CONTRAINDICATED;
    }

    private RiskLevel riskLevel(int finalScore, boolean blockedByRule, Optional<RiskLevel> worstAllergy) {
        if (isContraindicated(blockedByRule, worstAllergy)) {
            return RiskLevel.CRITICAL;
        }

        if (worstAllergyIs(worstAllergy, RiskLevel.MEDIUM) || finalScore < NOT_RECOMMENDED_FROM) {
            return RiskLevel.HIGH;
        }

        if (worstAllergyIs(worstAllergy, RiskLevel.LOW) || finalScore < ACCEPTABLE_FROM) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    private boolean isContraindicated(boolean blockedByRule, Optional<RiskLevel> worstAllergy) {
        return blockedByRule || worstAllergy.filter(CONTRAINDICATING_SEVERITIES::contains).isPresent();
    }

    private boolean worstAllergyIs(Optional<RiskLevel> worstAllergy, RiskLevel severity) {
        return worstAllergy.filter(severity::equals).isPresent();
    }
}
