package com.venus.classificacao.service.question;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import java.util.Set;

public interface ProfileQuestionStrategy {

    Set<Long> activeTagIds(ProfileSnapshot profile);

    QuestionScore score(ProductSnapshot product, ProfileSnapshot profile);
}
