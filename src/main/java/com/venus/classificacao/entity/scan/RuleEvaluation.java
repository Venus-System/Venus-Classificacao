package com.venus.classificacao.entity.scan;

import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import com.venus.classificacao.entity.ingredient.Ingredient;
import com.venus.classificacao.entity.shared.BaseEntity;
import com.venus.classificacao.entity.shared.ProfileTag;
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
@Table(name = "rule_evaluations")
@AttributeOverride(name = "id", column = @Column(name = "rule_evaluation_id"))
public class RuleEvaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_analysis_result_id", nullable = false)
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_compatibility_rule_id", nullable = false)
    private CompatibilityRule compatibilityRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_profile_tag_id", nullable = false)
    private ProfileTag profileTag;

    @Column(name = "was_matched", nullable = false)
    private Boolean wasMatched;

    @Column(name = "score_delta", nullable = false)
    private BigDecimal scoreDelta;

    @Column(name = "final_delta", nullable = false)
    private BigDecimal finalDelta;

    @Column(name = "explanation", nullable = false)
    private String explanation;
}
