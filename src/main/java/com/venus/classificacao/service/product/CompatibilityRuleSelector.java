package com.venus.classificacao.service.product;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CompatibilityRuleSelector {

    public List<CompatibilityRule> select(List<CompatibilityRule> rules, Long requestedModelId, Long baseModelId) {
        Map<Long, List<CompatibilityRule>> rulesByEffectId = rules.stream()
                .collect(Collectors.groupingBy(rule -> rule.getIngredientEffect().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        List<CompatibilityRule> selectedRules = new ArrayList<>();
        for (List<CompatibilityRule> effectRules : rulesByEffectId.values()) {
            ruleThatApplies(effectRules, requestedModelId, baseModelId).ifPresent(selectedRules::add);
        }

        return selectedRules;
    }

    private Optional<CompatibilityRule> ruleThatApplies(List<CompatibilityRule> effectRules, Long requestedModelId,
            Long baseModelId) {
        Optional<CompatibilityRule> baseModelRule = ruleOfModel(effectRules, baseModelId);
        if (baseModelRule.filter(this::isBlock).isPresent()) {
            return baseModelRule;
        }

        Optional<CompatibilityRule> requestedModelRule = ruleOfModel(effectRules, requestedModelId);
        if (requestedModelRule.isPresent()) {
            return requestedModelRule;
        }

        return baseModelRule;
    }

    private Optional<CompatibilityRule> ruleOfModel(List<CompatibilityRule> effectRules, Long scoringModelId) {
        return effectRules.stream()
                .filter(rule -> rule.getScoringModel().getId().equals(scoringModelId))
                .findFirst();
    }

    private boolean isBlock(CompatibilityRule rule) {
        return rule.getEffectType() == EffectType.BLOCK;
    }
}
