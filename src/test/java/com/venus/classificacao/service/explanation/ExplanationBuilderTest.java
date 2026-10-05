package com.venus.classificacao.service.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.QualityResult;
import com.venus.classificacao.service.question.MatchedRule;
import com.venus.classificacao.service.question.ProfileResult;
import com.venus.classificacao.service.question.QuestionScore;
import com.venus.classificacao.service.verdict.AllergyMatch;
import com.venus.classificacao.service.verdict.CombinedScore;
import com.venus.classificacao.service.verdict.Evaluation;
import com.venus.classificacao.service.verdict.Verdict;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExplanationBuilderTest {

    private static final String BLOCK_TEXT = "Lilial é proibido na gestação.";
    private static final String ALLERGY_TEXT = "Contém Lanolina, ligado à sua alergia a Lanolina (gravidade alta).";
    private static final String PREFERENCE_TEXT = "A marca declara ser vegana, como você prefere.";

    private final ExplanationBuilder explanationBuilder = new ExplanationBuilder();

    @Test
    void ruleReasonsPutTheBlockFirstAndThenTheBiggestImpact() {
        List<Reason> reasons = explanationBuilder.ruleReasonsOf(threeRules());

        assertThat(reasons).extracting(Reason::text).containsExactly(
                BLOCK_TEXT,
                "Álcool resseca a pele.",
                "Niacinamida tem uma regra de bônus para pele-acneica.");
        assertThat(reasons).extracting(Reason::warning).containsExactly(true, true, false);
    }

    @Test
    void reasonsGoFromBlockToAllergyToPreferenceToRulesToQuality() {
        Explanation explanation = explanationBuilder.build(contraindicatedEvaluation());

        assertThat(explanation.reasons()).extracting(Reason::source).containsExactly(
                ReasonSource.INGREDIENT_RULE,
                ReasonSource.ALLERGY,
                ReasonSource.PREFERENCE,
                ReasonSource.INGREDIENT_RULE,
                ReasonSource.INGREDIENT_RULE,
                ReasonSource.BASE_SCORE);
        assertThat(explanation.reasons()).extracting(Reason::text).containsExactly(
                BLOCK_TEXT,
                ALLERGY_TEXT,
                PREFERENCE_TEXT,
                "Álcool resseca a pele.",
                "Niacinamida tem uma regra de bônus para pele-acneica.",
                "Qualidade do produto: 74 de 100.");
    }

    @Test
    void summaryCitesTheTwoMainReasonsAndMarksTheWarnings() {
        Explanation explanation = explanationBuilder.build(contraindicatedEvaluation());

        assertThat(explanation.summary()).isEqualTo("Nota 0 de 100 - Contraindicado. "
                + "Atenção: " + BLOCK_TEXT + " Atenção: " + ALLERGY_TEXT);
    }

    @Test
    void unevaluatedIngredientsBecomeAQualityReason() {
        Evaluation evaluation = evaluation(List.of(), List.of(), List.of(),
                new Verdict(74, RecommendationLevel.RECOMMENDED, RiskLevel.LOW), 3);

        Explanation explanation = explanationBuilder.build(evaluation);

        assertThat(explanation.reasons()).extracting(Reason::text).containsExactly(
                "Qualidade do produto: 74 de 100.",
                "3 de 18 ingredientes ainda sem avaliação de saúde e ambiental.");
        assertThat(explanation.summary()).isEqualTo("Nota 74 de 100 - Recomendado.");
    }

    private static Evaluation contraindicatedEvaluation() {
        return evaluation(threeRules(), List.of(PREFERENCE_TEXT),
                List.of(new AllergyMatch("Lanolina", RiskLevel.HIGH, List.of("Lanolina"))),
                new Verdict(0, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL), 0);
    }

    private static List<MatchedRule> threeRules() {
        return List.of(
                rule(1L, "Niacinamida", EffectType.BONUS, 5, ""),
                rule(2L, "Álcool", EffectType.PENALTY, -10, "Álcool resseca a pele."),
                rule(3L, "Lilial", EffectType.BLOCK, 0, BLOCK_TEXT));
    }

    private static Evaluation evaluation(List<MatchedRule> matchedRules, List<String> preferenceNotes,
            List<AllergyMatch> allergies, Verdict verdict, int unevaluatedIngredientCount) {
        QualityResult qualityResult = new QualityResult(Map.of(), 74.0);
        ProfileResult profileResult = new ProfileResult(List.of(
                new QuestionScore("perguntas", 10, true, 0, matchedRules, preferenceNotes)));
        CombinedScore combinedScore = CombinedScore.of(qualityResult, profileResult);
        return new Evaluation(qualityResult, profileResult, combinedScore, allergies, verdict, 18,
                unevaluatedIngredientCount);
    }

    private static MatchedRule rule(Long ruleId, String ingredientName, EffectType effectType, int scoreDelta,
            String reason) {
        return MatchedRule.of(new ProductSnapshot.RuleData(ruleId, ruleId, ingredientName, 100L, "pele-acneica",
                effectType, scoreDelta, BigDecimal.ONE, reason));
    }
}
