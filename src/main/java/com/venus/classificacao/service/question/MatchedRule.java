package com.venus.classificacao.service.question;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record MatchedRule(
        Long ruleId,
        Long ingredientId,
        String ingredientName,
        Long profileTagId,
        String profileTagSlug,
        EffectType effectType,
        int scoreDelta,
        BigDecimal weight,
        BigDecimal impact,
        String reason
) {

    private static final int IMPACT_DECIMALS = 2;

    public static MatchedRule of(ProductSnapshot.RuleData rule) {
        return new MatchedRule(
                rule.ruleId(),
                rule.ingredientId(),
                rule.ingredientName(),
                rule.profileTagId(),
                rule.profileTagSlug(),
                rule.effectType(),
                rule.scoreDelta(),
                rule.weight(),
                impactOf(rule),
                rule.reason());
    }

    public boolean isScoring() {
        return effectType == EffectType.BONUS || effectType == EffectType.PENALTY;
    }

    public boolean isBlock() {
        return effectType == EffectType.BLOCK;
    }

    public boolean isWarning() {
        return effectType == EffectType.BLOCK || effectType == EffectType.PENALTY;
    }

    public String explanationText() {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        return ingredientName + " tem uma regra de " + effectLabel() + " para " + profileTagSlug + ".";
    }

    private String effectLabel() {
        return switch (effectType) {
            case BONUS -> "bônus";
            case PENALTY -> "penalidade";
            case BLOCK -> "bloqueio";
            case NEUTRAL, ALERT -> "informação";
        };
    }

    private static BigDecimal impactOf(ProductSnapshot.RuleData rule) {
        BigDecimal impactSize = BigDecimal.valueOf(rule.scoreDelta())
                .multiply(rule.weight())
                .abs()
                .setScale(IMPACT_DECIMALS, RoundingMode.HALF_UP);

        return switch (rule.effectType()) {
            case BONUS -> impactSize;
            case PENALTY -> impactSize.negate();
            case BLOCK, NEUTRAL, ALERT -> BigDecimal.ZERO.setScale(IMPACT_DECIMALS);
        };
    }
}
