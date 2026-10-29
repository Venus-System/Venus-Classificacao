package com.venus.classificacao.repository.ingredient;

import com.venus.classificacao.entity.ingredient.AllergyIngredient;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergyIngredientRepository extends JpaRepository<AllergyIngredient, Long> {

    @EntityGraph(attributePaths = "ingredient")
    List<AllergyIngredient> findByAllergyIdIn(Collection<Long> allergyIds);
}
