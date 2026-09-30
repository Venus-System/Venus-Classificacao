package com.venus.classificacao.entity.scoring;

import com.venus.classificacao.entity.product.ProductVersion;
import com.venus.classificacao.entity.shared.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "product_scores")
@AttributeOverride(name = "id", column = @Column(name = "product_score_id"))
public class ProductScore extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_product_version_id", nullable = false)
    private ProductVersion productVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_scoring_model_id", nullable = false)
    private ScoringModel scoringModel;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "environmental_score")
    private Integer environmentalScore;

    @Column(name = "ethical_score", nullable = false)
    private Integer ethicalScore;

    @Column(name = "performance_score", nullable = false)
    private Integer performanceScore;

    @Column(name = "transparency_score")
    private Integer transparencyScore;

    @Column(name = "confidence_score")
    private Short confidenceScore;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;
}
