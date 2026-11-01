package com.venus.classificacao.service.question.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileResult;
import com.venus.classificacao.service.question.ProfileTagSlugs;
import com.venus.classificacao.service.question.QuestionScore;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HealthStrategiesTest {

    private static final long PREGNANCY_TAG_ID = 50L;
    private static final long BREASTFEEDING_TAG_ID = 51L;

    private final PregnancyStrategy pregnancy = new PregnancyStrategy();
    private final BreastfeedingStrategy breastfeeding = new BreastfeedingStrategy();

    @Test
    void pregnancyBlockStopsProduct() {
        ProductSnapshot product = productWith(rule(PREGNANCY_TAG_ID, ProfileTagSlugs.PREGNANCY, EffectType.BLOCK, 0));

        QuestionScore score = pregnancy.score(product, profile(true, false));

        assertThat(score.matchedRules()).hasSize(1);
        assertThat(new ProfileResult(List.of(score)).hasBlock()).isTrue();
    }

    @Test
    void notPregnantSkipsRules() {
        ProductSnapshot product = productWith(rule(PREGNANCY_TAG_ID, ProfileTagSlugs.PREGNANCY, EffectType.BLOCK, 0));

        QuestionScore score = pregnancy.score(product, profile(false, false));

        assertThat(score.applicable()).isFalse();
        assertThat(score.matchedRules()).isEmpty();
        assertThat(new ProfileResult(List.of(score)).hasBlock()).isFalse();
    }

    @Test
    void breastfeedingBonusAddsPoints() {
        ProductSnapshot product = productWith(
                rule(BREASTFEEDING_TAG_ID, ProfileTagSlugs.BREASTFEEDING, EffectType.BONUS, 10));

        QuestionScore score = breastfeeding.score(product, profile(false, true));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isEqualTo(2.5);
    }

    @Test
    void notBreastfeedingSkipsRules() {
        ProductSnapshot product = productWith(
                rule(BREASTFEEDING_TAG_ID, ProfileTagSlugs.BREASTFEEDING, EffectType.BONUS, 10));

        QuestionScore score = breastfeeding.score(product, profile(false, false));

        assertThat(score.applicable()).isFalse();
        assertThat(score.earnedPoints()).isZero();
    }

    private static ProfileSnapshot profile(boolean pregnant, boolean breastfeeding) {
        ProfileSnapshot.Answers answers = new ProfileSnapshot.Answers(null, null, false, false, false, false, false,
                pregnant, breastfeeding, false, false, false, false, false);
        Map<String, Long> tagIdBySlug = Map.of(
                ProfileTagSlugs.PREGNANCY, PREGNANCY_TAG_ID,
                ProfileTagSlugs.BREASTFEEDING, BREASTFEEDING_TAG_ID);
        return new ProfileSnapshot(1L, answers, tagIdBySlug, List.of(), List.of());
    }

    private static ProductSnapshot productWith(ProductSnapshot.RuleData rule) {
        ProductSnapshot.BrandClaims claims = new ProductSnapshot.BrandClaims(ProductSnapshot.ClaimStatus.UNKNOWN,
                ProductSnapshot.ClaimStatus.UNKNOWN);
        return new ProductSnapshot(1L, claims, List.of(), Optional.empty(), 0, Map.of(), Set.of(), List.of(rule));
    }

    private static ProductSnapshot.RuleData rule(long tagId, String tagSlug, EffectType effectType, int scoreDelta) {
        return new ProductSnapshot.RuleData(1L, 10L, "Acido Salicilico", "Ativos", tagId, tagSlug, tagSlug,
                effectType, "Contraindicado", scoreDelta, BigDecimal.ONE, "");
    }
}
