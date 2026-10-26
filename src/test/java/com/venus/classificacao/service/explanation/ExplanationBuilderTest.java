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
    private static final String ALLERGY_TEXT =
            "Tem Lanolina, que está na sua alergia a Lanolina. Por isso esse produto não é indicado para você.";
    private static final String QUALITY_TEXT = "A qualidade do produto, sem olhar o seu perfil, é 74 de 100.";
    private static final String PREFERENCE_TEXT = "A marca declara ser vegana, como você prefere.";
    private static final String COMEDOGENIC = "Potencial comedogênico alto";
    private static final String IRRITATION = "Risco de irritação elevado";
    private static final String BONUS_TEXT =
            "Ativos com controle da acne. Pelo seu perfil (tendência a acne), isso somou pontos na nota.";

    private final ExplanationBuilder explanationBuilder = new ExplanationBuilder();

    @Test
    void ruleReasonsPutTheBlockFirstAndThenTheBiggestImpact() {
        List<Reason> reasons = explanationBuilder.ruleReasonsOf(threeRules());

        assertThat(reasons).extracting(Reason::text).containsExactly(
                BLOCK_TEXT,
                "Álcool resseca a pele.",
                BONUS_TEXT);
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
                BONUS_TEXT,
                QUALITY_TEXT);
    }

    @Test
    void summaryCitesTheTwoMainReasonsAndWarnsOnce() {
        Explanation explanation = explanationBuilder.build(contraindicatedEvaluation());

        assertThat(explanation.summary()).isEqualTo("Nota 0 de 100 - Contraindicado. "
                + "Atenção: " + BLOCK_TEXT + " " + ALLERGY_TEXT);
    }

    @Test
    void summaryGroupsRulesWithTheSameEffect() {
        List<MatchedRule> rules = List.of(
                penalty(1L, 1L, COMEDOGENIC, "pele-acneica", "Pele Acneica", -10),
                penalty(2L, 2L, COMEDOGENIC, "pele-acneica", "Pele Acneica", -10),
                penalty(3L, 3L, COMEDOGENIC, "pele-acneica", "Pele Acneica", -10),
                penalty(4L, 4L, IRRITATION, "reativa", "Reativa", -5),
                penalty(5L, 5L, IRRITATION, "reativa", "Reativa", -5));

        Explanation explanation = explanationBuilder.build(notRecommendedEvaluation(rules));

        assertThat(explanation.summary()).isEqualTo("Nota 25 de 100 - Não recomendado. "
                + "Atenção: 3 ingredientes com potencial comedogênico alto (tendência a acne). "
                + "2 ingredientes com risco de irritação elevado (pele sensível).");
    }

    @Test
    void summaryCountsIngredientsNotRules() {
        List<MatchedRule> rules = List.of(
                penalty(1L, 7L, COMEDOGENIC, "pele-acneica", "Pele Acneica", -10),
                penalty(2L, 7L, COMEDOGENIC, "pele-acneica", "Pele Acneica", -10));

        Explanation explanation = explanationBuilder.build(notRecommendedEvaluation(rules));

        assertThat(explanation.summary()).isEqualTo("Nota 25 de 100 - Não recomendado. "
                + "Atenção: 1 ingrediente com potencial comedogênico alto (tendência a acne).");
    }

    @Test
    void summaryNeverRepeatsASentence() {
        List<MatchedRule> rules = List.of(
                rule(1L, "Álcool", EffectType.PENALTY, -10, "Álcool resseca a pele."),
                rule(2L, "Etanol", EffectType.PENALTY, -10, "Álcool resseca a pele."));

        Explanation explanation = explanationBuilder.build(notRecommendedEvaluation(rules));

        assertThat(explanation.summary())
                .isEqualTo("Nota 25 de 100 - Não recomendado. Atenção: Álcool resseca a pele.");
    }

    @Test
    void unevaluatedIngredientsBecomeAQualityReason() {
        Evaluation evaluation = evaluation(List.of(), List.of(), List.of(),
                new Verdict(74, RecommendationLevel.RECOMMENDED, RiskLevel.LOW), 3);

        Explanation explanation = explanationBuilder.build(evaluation);

        assertThat(explanation.reasons()).extracting(Reason::text).containsExactly(
                QUALITY_TEXT,
                "3 dos 18 ingredientes ainda não foram avaliados, então ficaram fora da nota de qualidade.");
        assertThat(explanation.summary()).isEqualTo("Nota 74 de 100 - Recomendado.");
    }

    @Test
    void oneUnevaluatedIngredientIsSingular() {
        Evaluation evaluation = evaluation(List.of(), List.of(), List.of(),
                new Verdict(74, RecommendationLevel.RECOMMENDED, RiskLevel.LOW), 1);

        assertThat(explanationBuilder.build(evaluation).reasons()).extracting(Reason::text).contains(
                "1 dos 18 ingredientes ainda não foi avaliado, então ficou fora da nota de qualidade.");
    }

    @Test
    void highAllergyIsNotIndicated() {
        assertThat(allergyTextOf(new AllergyMatch("Lanolina", RiskLevel.HIGH, List.of("Lanolina"))))
                .isEqualTo(ALLERGY_TEXT);
    }

    @Test
    void criticalAllergyIsNotIndicated() {
        assertThat(allergyTextOf(new AllergyMatch("Lanolina", RiskLevel.CRITICAL, List.of("Lanolina"))))
                .isEqualTo(ALLERGY_TEXT);
    }

    @Test
    void mediumAllergyCapsTheScore() {
        assertThat(allergyTextOf(new AllergyMatch("Fragrância", RiskLevel.MEDIUM, List.of("LIMONENO", "LINALOL"))))
                .isEqualTo("Tem LIMONENO e LINALOL, que estão na sua alergia a Fragrância. "
                        + "Por isso a nota fica no máximo em 49.");
    }

    @Test
    void lowAllergyTakesPoints() {
        assertThat(allergyTextOf(new AllergyMatch("Fragrância", RiskLevel.LOW, List.of("A", "B", "C"))))
                .isEqualTo("Tem A, B e C, que estão na sua alergia a Fragrância. Por isso a nota caiu 15 pontos.");
    }

    @Test
    void allergyReasonListsTheIngredients() {
        Evaluation evaluation = evaluation(threeRules(), List.of(PREFERENCE_TEXT),
                List.of(new AllergyMatch("Fragrância", RiskLevel.MEDIUM, List.of("LIMONENO", "LINALOL"))),
                new Verdict(0, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL), 0);

        List<Reason> reasons = explanationBuilder.build(evaluation).reasons();

        assertThat(reasons).filteredOn(reason -> reason.source() == ReasonSource.ALLERGY)
                .extracting(Reason::ingredientNames)
                .containsExactly(List.of("LIMONENO", "LINALOL"));
        assertThat(reasons).filteredOn(reason -> reason.source() != ReasonSource.ALLERGY)
                .extracting(Reason::ingredientNames)
                .containsOnlyNulls();
    }

    private String allergyTextOf(AllergyMatch match) {
        Evaluation evaluation = evaluation(List.of(), List.of(), List.of(match),
                new Verdict(15, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL), 0);

        return explanationBuilder.build(evaluation).reasons().stream()
                .filter(reason -> reason.source() == ReasonSource.ALLERGY)
                .map(Reason::text)
                .findFirst()
                .orElseThrow();
    }

    private static Evaluation contraindicatedEvaluation() {
        return evaluation(threeRules(), List.of(PREFERENCE_TEXT),
                List.of(new AllergyMatch("Lanolina", RiskLevel.HIGH, List.of("Lanolina"))),
                new Verdict(0, RecommendationLevel.CONTRAINDICATED, RiskLevel.CRITICAL), 0);
    }

    private static Evaluation notRecommendedEvaluation(List<MatchedRule> rules) {
        return evaluation(rules, List.of(), List.of(),
                new Verdict(25, RecommendationLevel.NOT_RECOMMENDED, RiskLevel.HIGH), 0);
    }

    private static MatchedRule penalty(Long ruleId, Long ingredientId, String effectName, String tagSlug,
            String tagName, int scoreDelta) {
        return MatchedRule.of(new ProductSnapshot.RuleData(ruleId, ingredientId, "Ingrediente " + ingredientId,
                "Emoliente", 100L, tagSlug, tagName, EffectType.PENALTY, effectName, scoreDelta, BigDecimal.ONE, ""));
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
        return MatchedRule.of(new ProductSnapshot.RuleData(ruleId, ruleId, ingredientName, "Ativos", 100L,
                "pele-acneica", "Pele Acneica", effectType, "Controle da acne", scoreDelta, BigDecimal.ONE, reason));
    }
}
