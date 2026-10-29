package com.venus.classificacao.service.question.skin;

import com.venus.classificacao.entity.enums.SkinType;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.ProfileTagSlugs;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SkinTypeStrategy implements ProfileQuestionStrategy {

    private static final String KEY = "skin-type";
    private static final int MAX_POINTS = 12;

    @Override
    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        String tagSlug = tagSlugOf(profile.answers().skinType());
        return profile.tagIdsOf(tagSlug);
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

    private String tagSlugOf(SkinType skinType) {
        if (skinType == null) {
            return null;
        }

        return switch (skinType) {
            case ACNEIC -> ProfileTagSlugs.ACNEIC_SKIN;
            case DRY -> ProfileTagSlugs.VERY_DRY_SKIN;
            case OILY -> ProfileTagSlugs.OIL_CONTROL;
            case SENSITIVE -> ProfileTagSlugs.REACTIVE_SKIN;
            case NORMAL, COMBINATION, OTHER -> null;
        };
    }
}
