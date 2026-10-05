package com.venus.classificacao.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionScoreTest {

    private static final String KEY = "acne-prone";
    private static final int MAX_POINTS = 7;

    @Test
    void bonusOfFiveEarnsAQuarterOfTheQuestion() {
        QuestionScore score = QuestionScore.fromRules(KEY, MAX_POINTS, List.of(rule(1L, EffectType.BONUS, 5)));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isEqualTo(1.75);
    }

    @Test
    void bonusAboveTwentyIsCappedAtTheQuestionMaximum() {
        List<ProductSnapshot.RuleData> rules = List.of(rule(1L, EffectType.BONUS, 15), rule(2L, EffectType.BONUS, 10));

        assertThat(QuestionScore.fromRules(KEY, MAX_POINTS, rules).earnedPoints()).isEqualTo(7.0);
    }

    @Test
    void bonusAndPenaltyAreAddedBeforeTheRuler() {
        List<ProductSnapshot.RuleData> rules = List.of(rule(1L, EffectType.BONUS, 10), rule(2L, EffectType.PENALTY, -4));

        assertThat(QuestionScore.fromRules(KEY, MAX_POINTS, rules).earnedPoints()).isCloseTo(2.1, within(1e-9));
    }

    @Test
    void penaltyAloneNeverGoesBelowZero() {
        QuestionScore score = QuestionScore.fromRules(KEY, MAX_POINTS, List.of(rule(1L, EffectType.PENALTY, -5)));

        assertThat(score.earnedPoints()).isZero();
    }

    @Test
    void questionWithOnlyNeutralAndBlockRulesLeavesTheAccountButKeepsTheRules() {
        List<ProductSnapshot.RuleData> rules = List.of(rule(1L, EffectType.NEUTRAL, 0), rule(2L, EffectType.BLOCK, 0));

        QuestionScore score = QuestionScore.fromRules(KEY, MAX_POINTS, rules);

        assertThat(score.applicable()).isFalse();
        assertThat(score.matchedRules()).hasSize(2);
    }

    @Test
    void brandClaimGivesAllOrNothing() {
        assertThat(QuestionScore.fromBrandClaim("vegan", 3, true, "nota").earnedPoints()).isEqualTo(3.0);
        assertThat(QuestionScore.fromBrandClaim("vegan", 3, false, "nota").earnedPoints()).isZero();
    }

    private static ProductSnapshot.RuleData rule(Long ruleId, EffectType effectType, int scoreDelta) {
        return new ProductSnapshot.RuleData(ruleId, 10L, "Niacinamida", 100L, "pele-acneica", effectType, scoreDelta,
                BigDecimal.ONE, "");
    }
}
