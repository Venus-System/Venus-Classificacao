package com.venus.classificacao.entity.ingredient;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.enums.EvidenceLevel;
import com.venus.classificacao.entity.enums.SourceType;
import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.entity.shared.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "compatibility_rules")
@AttributeOverride(name = "id", column = @Column(name = "compatibility_rule_id"))
public class CompatibilityRule extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_ingredient_effect_id", nullable = false)
    private IngredientEffect ingredientEffect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_scoring_model_id", nullable = false)
    private ScoringModel scoringModel;

    @Column(name = "effect_type", nullable = false)
    private EffectType effectType;

    @Column(name = "score_delta", nullable = false)
    private Integer scoreDelta;

    @Column(name = "weight", nullable = false, precision = 6, scale = 2)
    private BigDecimal weight;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "has_concentration_factor", nullable = false)
    private Boolean hasConcentrationFactor;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "evidence_level", nullable = false)
    private EvidenceLevel evidenceLevel;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_reference")
    private String sourceReference;
}
