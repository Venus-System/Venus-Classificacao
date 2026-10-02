package com.venus.classificacao.service.question;

import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import java.util.List;

public record QuestionScore(
        String questionKey,
        int maxPoints,
        boolean applicable,
        double earnedPoints,
        List<MatchedRule> matchedRules,
        List<String> preferenceNotes
) {

    private static final double BONUS_FOR_FULL_POINTS = 20;

    public QuestionScore {
        matchedRules = List.copyOf(matchedRules);
        preferenceNotes = List.copyOf(preferenceNotes);
    }

    public static QuestionScore notApplicable(String questionKey, int maxPoints) {
        return new QuestionScore(questionKey, maxPoints, false, 0, List.of(), List.of());
    }

    public static QuestionScore fromRules(String questionKey, int maxPoints, List<ProductSnapshot.RuleData> rules) {
        List<MatchedRule> matchedRules = rules.stream()
                .map(MatchedRule::of)
                .toList();

        boolean hasScoringRule = matchedRules.stream().anyMatch(MatchedRule::isScoring);
        if (!hasScoringRule) {
            return new QuestionScore(questionKey, maxPoints, false, 0, matchedRules, List.of());
        }

        double impactSum = matchedRules.stream()
                .filter(MatchedRule::isScoring)
                .map(MatchedRule::impact)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        double points = maxPoints * (impactSum / BONUS_FOR_FULL_POINTS);
        double earnedPoints = Math.max(0, Math.min(maxPoints, points));

        return new QuestionScore(questionKey, maxPoints, true, earnedPoints, matchedRules, List.of());
    }

    public static QuestionScore fromBrandClaim(String questionKey, int maxPoints, boolean declaredByBrand,
            String note) {
        double earnedPoints = declaredByBrand ? maxPoints : 0;
        return new QuestionScore(questionKey, maxPoints, true, earnedPoints, List.of(), List.of(note));
    }
}
