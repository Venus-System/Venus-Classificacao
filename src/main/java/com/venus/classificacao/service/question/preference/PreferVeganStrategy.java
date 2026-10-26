package com.venus.classificacao.service.question.preference;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PreferVeganStrategy implements ProfileQuestionStrategy {

    private static final String KEY = "vegan";
    private static final int MAX_POINTS = 3;

    private static final String DECLARED_NOTE =
            "A marca diz que é vegana, como você prefere, e isso somou pontos na nota.";
    private static final String NOT_DECLARED_NOTE =
            "A marca não diz que é vegana, e você prefere produtos veganos, por isso essa parte não somou pontos.";
    private static final String UNKNOWN_NOTE =
            "A marca não informa se é vegana, então essa preferência não entrou na nota.";

    @Override
    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        return Set.of();
    }

    @Override
    public QuestionScore score(ProductSnapshot product, ProfileSnapshot profile) {
        if (!profile.answers().prefersVegan()) {
            return QuestionScore.notApplicable(KEY, MAX_POINTS);
        }

        ProductSnapshot.ClaimStatus veganClaim = product.brandClaims().vegan();
        if (veganClaim == ProductSnapshot.ClaimStatus.UNKNOWN) {
            return QuestionScore.fromUnknownClaim(KEY, MAX_POINTS, UNKNOWN_NOTE);
        }

        boolean declaredByBrand = veganClaim == ProductSnapshot.ClaimStatus.DECLARED;
        String note = declaredByBrand ? DECLARED_NOTE : NOT_DECLARED_NOTE;

        return QuestionScore.fromBrandClaim(KEY, MAX_POINTS, declaredByBrand, note);
    }
}
