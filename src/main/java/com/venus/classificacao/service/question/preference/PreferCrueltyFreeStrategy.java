package com.venus.classificacao.service.question.preference;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PreferCrueltyFreeStrategy implements ProfileQuestionStrategy {

    private static final String KEY = "cruelty-free";
    private static final int MAX_POINTS = 2;

    private static final String DECLARED_NOTE =
            "A marca declara não testar em animais, como você prefere.";
    private static final String NOT_DECLARED_NOTE =
            "A marca não declara que não testa em animais, e você prefere produtos cruelty-free.";

    @Override
    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        return Set.of();
    }

    @Override
    public QuestionScore score(ProductSnapshot product, ProfileSnapshot profile) {
        if (!profile.answers().prefersCrueltyFree()) {
            return QuestionScore.notApplicable(KEY, MAX_POINTS);
        }

        boolean declaredByBrand = product.brandClaims().crueltyFree();
        String note = declaredByBrand ? DECLARED_NOTE : NOT_DECLARED_NOTE;

        return QuestionScore.fromBrandClaim(KEY, MAX_POINTS, declaredByBrand, note);
    }
}
