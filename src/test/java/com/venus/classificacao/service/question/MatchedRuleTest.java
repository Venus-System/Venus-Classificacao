package com.venus.classificacao.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MatchedRuleTest {

    private static final String IRRITATION = "Risco de irritação elevado";
    private static final String PENALTY_TEXT =
            "Conservante com risco de irritação elevado. Pelo seu perfil (pele sensível), isso tirou pontos da nota.";

    @Test
    void penaltyTookPoints() {
        assertThat(rule(EffectType.PENALTY, IRRITATION, "").explanationText()).isEqualTo(PENALTY_TEXT);
    }

    @Test
    void bonusAddedPoints() {
        assertThat(rule(EffectType.BONUS, "Suporte à barreira cutânea", "").explanationText()).isEqualTo(
                "Conservante com suporte à barreira cutânea. Pelo seu perfil (pele sensível), isso somou pontos na nota.");
    }

    @Test
    void blockIsNotIndicated() {
        assertThat(rule(EffectType.BLOCK, IRRITATION, "").explanationText()).isEqualTo(
                "Conservante com risco de irritação elevado. Pelo seu perfil (pele sensível), "
                        + "esse produto não é indicado para você.");
    }

    @Test
    void neutralOnlyWarns() {
        assertThat(rule(EffectType.NEUTRAL, IRRITATION, "").explanationText()).isEqualTo(
                "Conservante com risco de irritação elevado. Pelo seu perfil (pele sensível), "
                        + "fica só de aviso, sem mudar a nota.");
    }

    @Test
    void ownReasonStays() {
        assertThat(rule(EffectType.PENALTY, IRRITATION, "Fenoxietanol irrita.").explanationText())
                .isEqualTo("Fenoxietanol irrita.");
    }

    @Test
    void blankReasonFallsBack() {
        assertThat(rule(EffectType.PENALTY, IRRITATION, "   ").explanationText()).isEqualTo(PENALTY_TEXT);
    }

    @Test
    void acronymStays() {
        assertThat(rule(EffectType.PENALTY, "UV alto", "").explanationText()).isEqualTo(
                "Conservante com UV alto. Pelo seu perfil (pele sensível), isso tirou pontos da nota.");
    }

    @Test
    void emptyEffectName() {
        assertThat(rule(EffectType.PENALTY, "", "").explanationText()).isEqualTo(
                "Conservante. Pelo seu perfil (pele sensível), isso tirou pontos da nota.");
    }

    private static MatchedRule rule(EffectType effectType, String effectName, String reason) {
        return MatchedRule.of(new ProductSnapshot.RuleData(1L, 10L, "FENOXIETANOL", "Conservante", 100L, "reativa",
                "Reativa", effectType, effectName, -5, BigDecimal.ONE, reason));
    }
}
