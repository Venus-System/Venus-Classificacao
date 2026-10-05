package com.venus.classificacao.service.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import com.venus.classificacao.entity.ingredient.IngredientEffect;
import com.venus.classificacao.entity.scoring.ScoringModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityRuleSelectorTest {

    private static final Long BASE_MODEL = 1L;
    private static final Long REQUESTED_MODEL = 15L;

    private final CompatibilityRuleSelector selector = new CompatibilityRuleSelector();

    @Test
    void requestedModelRuleWinsOverTheBaseModelRule() {
        CompatibilityRule baseRule = rule(1L, 100L, BASE_MODEL, EffectType.BONUS);
        CompatibilityRule requestedRule = rule(2L, 100L, REQUESTED_MODEL, EffectType.PENALTY);

        assertThat(selector.select(List.of(baseRule, requestedRule), REQUESTED_MODEL, BASE_MODEL))
                .containsExactly(requestedRule);
    }

    @Test
    void baseModelRuleFillsTheEffectsTheRequestedModelDoesNotHave() {
        CompatibilityRule baseOnlyRule = rule(1L, 100L, BASE_MODEL, EffectType.BONUS);
        CompatibilityRule requestedRule = rule(2L, 200L, REQUESTED_MODEL, EffectType.BONUS);

        assertThat(selector.select(List.of(baseOnlyRule, requestedRule), REQUESTED_MODEL, BASE_MODEL))
                .containsExactly(baseOnlyRule, requestedRule);
    }

    @Test
    void blockOfTheBaseModelAlwaysWins() {
        CompatibilityRule baseBlock = rule(1L, 100L, BASE_MODEL, EffectType.BLOCK);
        CompatibilityRule requestedBonus = rule(2L, 100L, REQUESTED_MODEL, EffectType.BONUS);

        assertThat(selector.select(List.of(requestedBonus, baseBlock), REQUESTED_MODEL, BASE_MODEL))
                .containsExactly(baseBlock);
    }

    private static CompatibilityRule rule(Long ruleId, Long effectId, Long scoringModelId, EffectType effectType) {
        IngredientEffect effect = new IngredientEffect();
        effect.setId(effectId);
        ScoringModel scoringModel = new ScoringModel();
        scoringModel.setId(scoringModelId);
        CompatibilityRule rule = new CompatibilityRule();
        rule.setId(ruleId);
        rule.setIngredientEffect(effect);
        rule.setScoringModel(scoringModel);
        rule.setEffectType(effectType);
        return rule;
    }
}
