package com.venus.classificacao.service.question.hair;

import com.venus.classificacao.entity.enums.ScalpType;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.ProfileTagSlugs;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ScalpTypeStrategy implements ProfileQuestionStrategy {

    private static final String KEY = "scalp-type";
    private static final int MAX_POINTS = 11;

    @Override
    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        String tagSlug = tagSlugOf(profile.answers().scalpType());
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

    private String tagSlugOf(ScalpType scalpType) {
        if (scalpType == null) {
            return null;
        }

        return switch (scalpType) {
            case DANDRUFF -> ProfileTagSlugs.DANDRUFF;
            case DRY -> ProfileTagSlugs.DRY_SCALP;
            case OILY -> ProfileTagSlugs.OILY_SCALP;
            case SENSITIVE -> ProfileTagSlugs.SENSITIVE_SCALP;
            case NORMAL, OTHER -> null;
        };
    }
}
