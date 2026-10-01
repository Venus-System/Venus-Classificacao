package com.venus.classificacao.entity.product;

import com.venus.classificacao.entity.shared.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "brands")
@AttributeOverride(name = "id", column = @Column(name = "brand_id"))
public class Brand extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country")
    private String country;

    @Column(name = "website")
    private String website;

    @Column(name = "has_cruelty_free_claim", nullable = false)
    private Boolean hasCrueltyFreeClaim;

    @Column(name = "has_vegan_claim", nullable = false)
    private Boolean hasVeganClaim;

    @Column(name = "is_brazilian", nullable = false)
    private Boolean isBrazilian;
}
