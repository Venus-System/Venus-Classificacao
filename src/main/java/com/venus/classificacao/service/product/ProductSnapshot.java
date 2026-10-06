package com.venus.classificacao.service.product;

import com.venus.classificacao.entity.enums.EffectType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ProductSnapshot(
        Long productVersionId,
        BrandClaims brandClaims,
        List<IngredientData> ingredients,
        Optional<PackagingData> packaging,
        int verifiedEthicalSealCount,
        Map<Long, Integer> benefitCountByIngredientId,
        List<RuleData> rules
) {

    public ProductSnapshot {
        ingredients = List.copyOf(ingredients);
        benefitCountByIngredientId = Map.copyOf(benefitCountByIngredientId);
        rules = List.copyOf(rules);
    }

    public List<IngredientData> evaluatedIngredients() {
        return ingredients.stream()
                .filter(IngredientData::evaluated)
                .toList();
    }

    public int unevaluatedIngredientCount() {
        return ingredients.size() - evaluatedIngredients().size();
    }

    public Optional<PackagingData> evaluatedPackaging() {
        return packaging.filter(PackagingData::evaluated);
    }

    public int benefitCountOf(Long ingredientId) {
        return benefitCountByIngredientId.getOrDefault(ingredientId, 0);
    }

    public List<RuleData> rulesFor(Set<Long> profileTagIds) {
        return rules.stream()
                .filter(rule -> profileTagIds.contains(rule.profileTagId()))
                .toList();
    }

    public record BrandClaims(boolean vegan, boolean crueltyFree) {
    }

    public record IngredientData(
            Long id,
            String name,
            int irritationRisk,
            int comedogenicity,
            int biodegradability,
            int environmentalRisk,
            boolean evaluated
    ) {

        public static final int MIN_NOTE = 0;
        public static final int MAX_NOTE = 10;
    }

    public record PackagingData(
            boolean recyclable,
            boolean refillable,
            boolean biodegradable,
            BigDecimal recycledContentPercentage,
            boolean evaluated
    ) {
    }

    public record RuleData(
            Long ruleId,
            Long ingredientId,
            String ingredientName,
            Long profileTagId,
            String profileTagSlug,
            EffectType effectType,
            int scoreDelta,
            BigDecimal weight,
            String reason
    ) {
    }
}
