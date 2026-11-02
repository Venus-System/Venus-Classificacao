package com.venus.classificacao.service.question.skin;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.enums.SkinType;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileTagSlugs;
import com.venus.classificacao.service.question.QuestionScore;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SkinStrategiesTest {

    private static final Map<String, Long> TAG_ID_BY_SLUG = Map.of(
            ProfileTagSlugs.ACNEIC_SKIN, 20L,
            ProfileTagSlugs.OIL_CONTROL, 21L,
            ProfileTagSlugs.ECZEMA, 22L,
            ProfileTagSlugs.ROSACEA, 23L,
            ProfileTagSlugs.HYPERPIGMENTATION, 24L,
            ProfileTagSlugs.MELASMA, 25L);

    private final AcneProneStrategy acneProne = new AcneProneStrategy();
    private final SkinTypeStrategy skinType = new SkinTypeStrategy();
    private final HasEczemaStrategy eczema = new HasEczemaStrategy();
    private final HasRosaceaStrategy rosacea = new HasRosaceaStrategy();
    private final HasHyperpigmentationStrategy hyperpigmentation = new HasHyperpigmentationStrategy();
    private final HasMelasmaStrategy melasma = new HasMelasmaStrategy();

    @Test
    void acneBonusAddsPoints() {
        ProductSnapshot product = productWith(rule(ProfileTagSlugs.ACNEIC_SKIN, EffectType.BONUS, 10));

        QuestionScore score = acneProne.score(product, profile(answers(null, true, false)));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isEqualTo(3.5);
    }

    @Test
    void noAcneSkipsRules() {
        ProductSnapshot product = productWith(rule(ProfileTagSlugs.ACNEIC_SKIN, EffectType.BONUS, 10));

        QuestionScore score = acneProne.score(product, profile(answers(null, false, false)));

        assertThat(score.applicable()).isFalse();
        assertThat(score.matchedRules()).isEmpty();
    }

    @Test
    void oilySkinUsesOilTag() {
        ProductSnapshot product = productWith(rule(ProfileTagSlugs.OIL_CONTROL, EffectType.BONUS, 10));

        QuestionScore score = skinType.score(product, profile(answers(SkinType.OILY, false, false)));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isEqualTo(6.0);
    }

    @Test
    void normalSkinSkipsRules() {
        ProductSnapshot product = productWith(rule(ProfileTagSlugs.OIL_CONTROL, EffectType.BONUS, 10));

        QuestionScore score = skinType.score(product, profile(answers(SkinType.NORMAL, false, false)));

        assertThat(score.applicable()).isFalse();
    }

    @Test
    void eczemaPenaltyStopsAtZero() {
        ProductSnapshot product = productWith(rule(ProfileTagSlugs.ECZEMA, EffectType.PENALTY, -20));

        QuestionScore score = eczema.score(product, profile(answers(null, false, true)));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isZero();
    }

    @Test
    void conditionsUseOwnTags() {
        ProfileSnapshot.Answers allMarked = new ProfileSnapshot.Answers(null, null, false, true, false, true, true,
                false, false, false, false, false, false, false);
        ProfileSnapshot profile = profile(allMarked);

        assertThat(rosacea.activeTagIds(profile)).containsExactly(23L);
        assertThat(hyperpigmentation.activeTagIds(profile)).containsExactly(24L);
        assertThat(melasma.activeTagIds(profile)).containsExactly(25L);
    }

    private static ProfileSnapshot.Answers answers(SkinType skin, boolean acneProne, boolean hasEczema) {
        return new ProfileSnapshot.Answers(skin, null, acneProne, false, hasEczema, false, false, false, false,
                false, false, false, false, false);
    }

    private static ProfileSnapshot profile(ProfileSnapshot.Answers answers) {
        return new ProfileSnapshot(1L, answers, TAG_ID_BY_SLUG, List.of(), List.of());
    }

    private static ProductSnapshot productWith(ProductSnapshot.RuleData rule) {
        ProductSnapshot.BrandClaims claims = new ProductSnapshot.BrandClaims(ProductSnapshot.ClaimStatus.UNKNOWN,
                ProductSnapshot.ClaimStatus.UNKNOWN);
        return new ProductSnapshot(1L, claims, List.of(), Optional.empty(), 0, Map.of(), Set.of(), List.of(rule));
    }

    private static ProductSnapshot.RuleData rule(String tagSlug, EffectType effectType, int scoreDelta) {
        return new ProductSnapshot.RuleData(1L, 10L, "Niacinamida", "Ativos", TAG_ID_BY_SLUG.get(tagSlug), tagSlug,
                tagSlug, effectType, "Controle da oleosidade", scoreDelta, BigDecimal.ONE, "");
    }
}
