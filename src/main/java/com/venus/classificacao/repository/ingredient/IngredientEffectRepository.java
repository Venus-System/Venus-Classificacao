package com.venus.classificacao.repository.ingredient;

import com.venus.classificacao.entity.enums.EffectCategory;
import com.venus.classificacao.entity.ingredient.IngredientEffect;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientEffectRepository extends JpaRepository<IngredientEffect, Long> {

    List<IngredientEffect> findByIngredientIdInAndEffectCategory(Collection<Long> ingredientIds, EffectCategory effectCategory);
}
