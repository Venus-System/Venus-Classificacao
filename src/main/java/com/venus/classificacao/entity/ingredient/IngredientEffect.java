package com.venus.classificacao.entity.ingredient;

import com.venus.classificacao.entity.enums.EffectCategory;
import com.venus.classificacao.entity.enums.EffectStrength;
import com.venus.classificacao.entity.enums.EvidenceLevel;
import com.venus.classificacao.entity.enums.ReviewStatus;
import com.venus.classificacao.entity.enums.SourceType;
import com.venus.classificacao.entity.shared.AuditableEntity;
import com.venus.classificacao.entity.shared.ProfileTag;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "ingredient_effects")
@AttributeOverride(name = "id", column = @Column(name = "ingredient_effect_id"))
public class IngredientEffect extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_profile_tag_id", nullable = false)
    private ProfileTag profileTag;

    @Column(name = "effect_category", nullable = false)
    private EffectCategory effectCategory;

    @Column(name = "effect_name", nullable = false)
    private String effectName;

    @Column(name = "effect_description", nullable = false)
    private String effectDescription;

    @Column(name = "effect_strength", nullable = false)
    private EffectStrength effectStrength;

    @Column(name = "evidence_level", nullable = false)
    private EvidenceLevel evidenceLevel;

    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_reference")
    private String sourceReference;
}
