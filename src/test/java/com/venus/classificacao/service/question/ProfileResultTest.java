package com.venus.classificacao.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileResultTest {

    @Test
    void compatibilityIsEarnedOverPossibleOfTheApplicableQuestions() {
        ProfileResult profileResult = new ProfileResult(List.of(
                answered("acne-prone", 7, 1.75),
                QuestionScore.notApplicable("rosacea", 4),
                answered("vegan", 3, 3)));

        assertThat(profileResult.compatibilityRatio().getAsDouble()).isCloseTo(0.475, within(1e-9));
    }

    @Test
    void withoutApplicableQuestionTheCompatibilityIsEmpty() {
        ProfileResult profileResult = new ProfileResult(List.of(QuestionScore.notApplicable("rosacea", 4)));

        assertThat(profileResult.compatibilityRatio()).isEmpty();
    }

    @Test
    void sameRuleInTwoQuestionsBecomesOneMatchedRule() {
        ProductSnapshot.RuleData acneRule = new ProductSnapshot.RuleData(1L, 10L, "Niacinamida", 100L, "pele-acneica",
                EffectType.BONUS, 5, BigDecimal.ONE, "");

        ProfileResult profileResult = new ProfileResult(List.of(
                QuestionScore.fromRules("skin-type", 9, List.of(acneRule)),
                QuestionScore.fromRules("acne-prone", 7, List.of(acneRule))));

        assertThat(profileResult.distinctMatchedRules()).hasSize(1);
    }

    private static QuestionScore answered(String key, int maxPoints, double earnedPoints) {
        return new QuestionScore(key, maxPoints, true, earnedPoints, List.of(), List.of());
    }
}
