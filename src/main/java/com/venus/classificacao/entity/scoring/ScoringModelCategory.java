package com.venus.classificacao.entity.scoring;

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
@Table(name = "scoring_model_categories")
@AttributeOverride(name = "id", column = @Column(name = "scoring_model_category_id"))
public class ScoringModelCategory extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_scoring_model_id", nullable = false)
    private ScoringModel scoringModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_score_category_id", nullable = false)
    private ScoreCategory scoreCategory;

    @Column(name = "weight", nullable = false)
    private BigDecimal weight;
}
