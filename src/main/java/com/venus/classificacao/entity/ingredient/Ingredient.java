package com.venus.classificacao.entity.ingredient;

import com.venus.classificacao.entity.enums.SourceType;
import com.venus.classificacao.entity.shared.AuditableEntity;
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
@Table(name = "ingredients")
@AttributeOverride(name = "id", column = @Column(name = "ingredient_id"))
public class Ingredient extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_ingredient_category_id", nullable = false)
    private IngredientCategory ingredientCategory;

    @Column(name = "inci_name", nullable = false, unique = true)
    private String inciName;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "function_summary", nullable = false)
    private String functionSummary;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "biodegradability_level", nullable = false)
    private Short biodegradabilityLevel;

    @Column(name = "irritation_risk_level", nullable = false)
    private Short irritationRiskLevel;

    @Column(name = "comedogenicity_score", nullable = false)
    private Short comedogenicityScore;

    @Column(name = "environmental_risk_level", nullable = false)
    private Short environmentalRiskLevel;

    @Column(name = "safety_summary", nullable = false)
    private String safetySummary;

    @Column(name = "scientific_confidence", nullable = false)
    private Short scientificConfidence;

    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_reference")
    private String sourceReference;
}
