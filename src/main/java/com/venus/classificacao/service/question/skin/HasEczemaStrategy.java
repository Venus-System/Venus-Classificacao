package com.venus.classificacao.service.question.skin;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.ProfileTagSlugs;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HasEczemaStrategy implements ProfileQuestionStrategy {

    private static final String KEY = "eczema";
    private static final int MAX_POINTS = 5;

    @Override
    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        if (!profile.answers().hasEczema()) {
            return Set.of();
        }
        return profile.tagIdsOf(ProfileTagSlugs.ECZEMA);
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
