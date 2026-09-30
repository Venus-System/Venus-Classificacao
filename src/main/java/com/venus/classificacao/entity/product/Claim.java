package com.venus.classificacao.entity.product;

import com.venus.classificacao.entity.enums.ClaimType;
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
@Table(name = "claims")
@AttributeOverride(name = "id", column = @Column(name = "claim_id"))
public class Claim extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "claim_type", nullable = false)
    private ClaimType claimType;
}
