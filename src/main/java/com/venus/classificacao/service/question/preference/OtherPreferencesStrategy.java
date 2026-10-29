package com.venus.classificacao.service.question.preference;

import com.venus.classificacao.entity.enums.ProfileTagCategory;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.ProfileTagSlugs;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OtherPreferencesStrategy implements ProfileQuestionStrategy {

    private static final String KEY = "other-preferences";
    private static final int MAX_POINTS = 2;

    private static final Set<ProfileTagCategory> PREFERENCE_CATEGORIES =
            EnumSet.of(ProfileTagCategory.VALUES, ProfileTagCategory.SUSTAINABILITY);

    private static final Set<String> SLUGS_WITH_PREFERENCE_FLAG = Set.of(
            ProfileTagSlugs.VEGAN,
            ProfileTagSlugs.CRUELTY_FREE,
            ProfileTagSlugs.FRAGRANCE_FREE,
            ProfileTagSlugs.PARABEN_FREE,
            ProfileTagSlugs.SULFATE_FREE,
            ProfileTagSlugs.SILICONE_FREE);

    @Override
    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        return profile.markedTags().stream()
                .filter(tag -> PREFERENCE_CATEGORIES.contains(tag.category()))
                .filter(tag -> !SLUGS_WITH_PREFERENCE_FLAG.contains(tag.slug()))
                .map(ProfileSnapshot.MarkedTag::id)
                .collect(Collectors.toSet());
    }

    @Override
    public QuestionScore score(ProductSnapshot product, ProfileSnapshot profile) {
        Set<Long> activeTagIds = activeTagIds(profile);
        if (activeTagIds.isEmpty()) {
            return QuestionScore.notApplicable(KEY, MAX_POINTS);
        }

        List<ProductSnapshot.RuleData> rules = product.rulesFor(activeTagIds);
        return QuestionScore.fromRules(KEY, MAX_POINTS, rules);
    }
}
