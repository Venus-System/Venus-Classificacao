package com.venus.classificacao.service.verdict;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FinalScorePolicyTest {

    private final FinalScorePolicy policy = new FinalScorePolicy();

    @ParameterizedTest
    @CsvSource({
            "85, IDEAL, LOW",
            "70, RECOMMENDED, LOW",
            "50, ACCEPTABLE, LOW",
            "49, NOT_RECOMMENDED, MEDIUM",
            "25, NOT_RECOMMENDED, MEDIUM",
            "24, CONTRAINDICATED, HIGH"
    })
    void scoreWithoutLocksFallsInItsBand(int score, RecommendationLevel level, RiskLevel risk) {
        assertThat(policy.decide(score, false, List.of())).isEqualTo(new Verdict(score, level, risk));
    }

    @Test
    void scoreIsRoundedAndKeptBetweenZeroAndOneHundred() {
        assertThat(policy.decide(84.5, false, List.of()).finalScore()).isEqualTo(85);
        assertThat(policy.decide(120, false, List.of()).finalScore()).isEqualTo(100);
        assertThat(policy.decide(-3, false, List.of()).finalScore()).isZero();
    }

    @Test
    void blockingRuleZeroesTheScore() {
        assertThat(policy.decide(90, true, List.of()))
                .isEqualTo(new Verdict(0, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL));
    }

    @Test
    void criticalAllergyZeroesTheScore() {
        assertThat(policy.decide(90, false, List.of(allergy(RiskLevel.CRITICAL))))
                .isEqualTo(new Verdict(0, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL));
    }

    @Test
    void highAllergyCapsAtFifteenAndContraindicates() {
        assertThat(policy.decide(90, false, List.of(allergy(RiskLevel.HIGH))))
                .isEqualTo(new Verdict(15, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL));
    }

    @Test
    void mediumAllergyNeverEndsBetterThanNotRecommended() {
        assertThat(policy.decide(100, false, List.of(allergy(RiskLevel.MEDIUM))))
                .isEqualTo(new Verdict(49, RecommendationLevel.NOT_RECOMMENDED, RiskLevel.HIGH));
    }

    @Test
    void lowAllergyTakesFifteenPoints() {
        assertThat(policy.decide(90, false, List.of(allergy(RiskLevel.LOW))))
                .isEqualTo(new Verdict(75, RecommendationLevel.RECOMMENDED, RiskLevel.MEDIUM));
    }

    @Test
    void theWorstAllergyWins() {
        assertThat(policy.decide(90, false, List.of(allergy(RiskLevel.LOW), allergy(RiskLevel.HIGH))))
                .isEqualTo(new Verdict(15, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL));
    }

    private static AllergyMatch allergy(RiskLevel severity) {
        return new AllergyMatch("Lanolina", severity, List.of("Lanolina"));
    }
}
