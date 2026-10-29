package com.venus.classificacao.service.profile;

import com.venus.classificacao.entity.enums.ProfileTagCategory;
import com.venus.classificacao.entity.enums.RiskLevel;
import com.venus.classificacao.entity.enums.ScalpType;
import com.venus.classificacao.entity.enums.SkinType;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProfileSnapshot(
        Long userId,
        Answers answers,
        Map<String, Long> tagIdBySlug,
        List<MarkedTag> markedTags,
        List<DeclaredAllergy> allergies
) {

    public ProfileSnapshot {
        tagIdBySlug = Map.copyOf(tagIdBySlug);
        markedTags = List.copyOf(markedTags);
        allergies = List.copyOf(allergies);
    }

    public Set<Long> tagIdsOf(String slug) {
        if (slug == null || !tagIdBySlug.containsKey(slug)) {
            return Set.of();
        }
        return Set.of(tagIdBySlug.get(slug));
    }

    public record Answers(
            SkinType skinType,
            ScalpType scalpType,
            boolean acneProne,
            boolean hasRosacea,
            boolean hasEczema,
            boolean hasHyperpigmentation,
            boolean hasMelasma,
            boolean pregnant,
            boolean breastfeeding,
            boolean prefersVegan,
            boolean prefersCrueltyFree,
            boolean prefersParabenFree,
            boolean prefersSulfateFree,
            boolean prefersSiliconeFree
    ) {
    }

    public record MarkedTag(Long id, String slug, ProfileTagCategory category) {
    }

    public record DeclaredAllergy(Long allergyId, String name, RiskLevel severity, Set<Long> ingredientIds) {

        public DeclaredAllergy {
            ingredientIds = Set.copyOf(ingredientIds);
        }
    }
}
