package com.venus.classificacao.entity.product;

import com.venus.classificacao.entity.enums.PackagingFormat;
import com.venus.classificacao.entity.enums.PackagingMaterial;
import com.venus.classificacao.entity.enums.SourceType;
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
@Table(name = "packaging")
@AttributeOverride(name = "id", column = @Column(name = "packaging_id"))
public class Packaging extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_product_version_id", nullable = false, unique = true)
    private ProductVersion productVersion;

    @Column(name = "material", nullable = false)
    private PackagingMaterial material;

    @Column(name = "material_detail", nullable = false)
    private String materialDetail;

    @Column(name = "packaging_format", nullable = false)
    private PackagingFormat packagingFormat;

    @Column(name = "is_recyclable", nullable = false)
    private Boolean isRecyclable;

    @Column(name = "is_refillable", nullable = false)
    private Boolean isRefillable;

    @Column(name = "is_biodegradable", nullable = false)
    private Boolean isBiodegradable;

    @Column(name = "recycled_content_percentage", precision = 5, scale = 2)
    private BigDecimal recycledContentPercentage;

    @Column(name = "confidence_score", nullable = false)
    private Short confidenceScore;

    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "was_manual_verified", nullable = false)
    private Boolean wasManualVerified;
}
