package com.venus.classificacao.repository.ingredient;

import com.venus.classificacao.entity.ingredient.ProductIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    @EntityGraph(attributePaths = "ingredient")
    List<ProductIngredient> findByProductVersionIdOrderByPosition(Long productVersionId);
}
