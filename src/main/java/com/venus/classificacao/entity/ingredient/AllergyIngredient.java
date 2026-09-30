package com.venus.classificacao.entity.ingredient;

import com.venus.classificacao.entity.enums.SourceType;
import com.venus.classificacao.entity.shared.AuditableEntity;
import com.venus.classificacao.entity.user.Allergy;
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
@Table(name = "allergy_ingredients")
@AttributeOverride(name = "id", column = @Column(name = "allergy_ingredient_id"))
public class AllergyIngredient extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_allergy_id", nullable = false)
    private Allergy allergy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_reference")
    private String sourceReference;
}
