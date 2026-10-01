package com.venus.classificacao.repository.ingredient;

import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Long> {

    @Query("""
            select r from CompatibilityRule r
            join fetch r.ingredientEffect e
            join fetch e.ingredient
            join fetch e.profileTag
            where r.scoringModel.id in :scoringModelIds
              and r.isEnabled = true
              and e.ingredient.id in :ingredientIds
              and e.profileTag.id in :profileTagIds
            """)
    List<CompatibilityRule> findEnabledRules(@Param("scoringModelIds") Collection<Long> scoringModelIds,
            @Param("ingredientIds") Collection<Long> ingredientIds,
            @Param("profileTagIds") Collection<Long> profileTagIds);
}
