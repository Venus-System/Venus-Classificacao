package com.venus.classificacao.entity.product;

import com.venus.classificacao.entity.enums.SourceType;
import com.venus.classificacao.entity.enums.VersionStatus;
import com.venus.classificacao.entity.shared.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "product_versions")
@AttributeOverride(name = "id", column = @Column(name = "product_version_id"))
public class ProductVersion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_product_id", nullable = false)
    private Product product;

    @Column(name = "version_name", nullable = false)
    private String versionName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "status", nullable = false)
    private VersionStatus status;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @Column(name = "formula_signature", nullable = false)
    private String formulaSignature;

    @Column(name = "detected_by", nullable = false)
    private SourceType detectedBy;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
