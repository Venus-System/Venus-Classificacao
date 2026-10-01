package com.venus.classificacao.entity.ingredient;

import com.venus.classificacao.entity.product.ProductVersion;
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
@Table(name = "product_ingredients")
@AttributeOverride(name = "id", column = @Column(name = "product_ingredient_id"))
public class ProductIngredient extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_product_version_id", nullable = false)
    private ProductVersion productVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "position", nullable = false)
    private Integer position;
}
