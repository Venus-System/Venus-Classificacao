package com.venus.classificacao.service.explanation;

import com.venus.classificacao.service.question.MatchedRule;
import java.math.BigDecimal;

public record Reason(
        ReasonSource source,
        String text,
        boolean warning,
        Integer scoreDelta,
        BigDecimal weight,
        BigDecimal impact,
        String ingredientName,
        String profileTagSlug
) {

    public static Reason ofRule(MatchedRule rule) {
        return new Reason(ReasonSource.INGREDIENT_RULE, rule.explanationText(), rule.isWarning(), rule.scoreDelta(),
                rule.weight(), rule.impact(), rule.ingredientName(), rule.profileTagSlug());
    }

    public static Reason ofAllergy(String text) {
        return new Reason(ReasonSource.ALLERGY, text, true, null, null, null, null, null);
    }

    public static Reason ofPreference(String text) {
        return new Reason(ReasonSource.PREFERENCE, text, false, null, null, null, null, null);
    }

    public static Reason ofBaseScore(String text) {
        return new Reason(ReasonSource.BASE_SCORE, text, false, null, null, null, null, null);
    }
}
