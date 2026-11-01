package com.venus.classificacao.service.question.hair;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.enums.ScalpType;
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

class ScalpTypeStrategyTest {

    private static final long DANDRUFF_TAG_ID = 30L;

    private final ScalpTypeStrategy scalpType = new ScalpTypeStrategy();

    @Test
    void dandruffUsesDandruffTag() {
        QuestionScore score = scalpType.score(productWithDandruffBonus(), profile(ScalpType.DANDRUFF));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isEqualTo(5.5);
    }

    @Test
    void normalScalpSkipsRules() {
        QuestionScore score = scalpType.score(productWithDandruffBonus(), profile(ScalpType.NORMAL));

        assertThat(score.applicable()).isFalse();
        assertThat(score.matchedRules()).isEmpty();
    }

    private static ProfileSnapshot profile(ScalpType scalp) {
        ProfileSnapshot.Answers answers = new ProfileSnapshot.Answers(null, scalp, false, false, false, false, false,
                false, false, false, false, false, false, false);
        return new ProfileSnapshot(1L, answers, Map.of(ProfileTagSlugs.DANDRUFF, DANDRUFF_TAG_ID), List.of(), List.of());
    }

    private static ProductSnapshot productWithDandruffBonus() {
        ProductSnapshot.RuleData rule = new ProductSnapshot.RuleData(1L, 10L, "Piritionato de Zinco", "Ativos",
                DANDRUFF_TAG_ID, ProfileTagSlugs.DANDRUFF, "Caspa", EffectType.BONUS, "Controle da caspa", 10,
                BigDecimal.ONE, "");
        ProductSnapshot.BrandClaims claims = new ProductSnapshot.BrandClaims(ProductSnapshot.ClaimStatus.UNKNOWN,
                ProductSnapshot.ClaimStatus.UNKNOWN);
        return new ProductSnapshot(1L, claims, List.of(), Optional.empty(), 0, Map.of(), Set.of(), List.of(rule));
    }
}
