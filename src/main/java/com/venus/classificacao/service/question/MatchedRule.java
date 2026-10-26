package com.venus.classificacao.service.question;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record MatchedRule(
        Long ruleId,
        Long ingredientId,
        String ingredientName,
        String categoryName,
        Long profileTagId,
        String profileTagSlug,
        String profileTagName,
        EffectType effectType,
        String effectName,
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
                rule.categoryName(),
                rule.profileTagId(),
                rule.profileTagSlug(),
                rule.profileTagName(),
                rule.effectType(),
                rule.effectName(),
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

    public boolean hasOwnReason() {
        return reason != null && !reason.isBlank();
    }

    public String profileLabel() {
        return ProfileTagLabels.labelOf(profileTagSlug, profileTagName);
    }

    public String effectText() {
        if (effectName == null || effectName.isBlank()) {
            return "";
        }
        if (effectName.length() > 1 && Character.isUpperCase(effectName.charAt(1))) {
            return effectName;
        }
        return Character.toLowerCase(effectName.charAt(0)) + effectName.substring(1);
    }

    public String explanationText() {
        if (hasOwnReason()) {
            return reason;
        }
        return ingredientText() + ". Pelo seu perfil (" + profileLabel() + "), " + outcomeText();
    }

    private String ingredientText() {
        String effect = effectText();
        if (effect.isEmpty()) {
            return categoryName;
        }
        return categoryName + " com " + effect;
    }

    private String outcomeText() {
        return switch (effectType) {
            case BONUS -> "isso somou pontos na nota.";
            case PENALTY -> "isso tirou pontos da nota.";
            case BLOCK -> "esse produto não é indicado para você.";
            case NEUTRAL, ALERT -> "fica só de aviso, sem mudar a nota.";
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
