package com.venus.classificacao.entity.user;

import com.venus.classificacao.entity.enums.AllergyType;
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
@Table(name = "allergies")
@AttributeOverride(name = "id", column = @Column(name = "allergy_id"))
public class Allergy extends AuditableEntity {

    @Column(name = "allergy_name", nullable = false, unique = true)
    private String allergyName;

    @Column(name = "allergy_type", nullable = false)
    private AllergyType allergyType;
}
