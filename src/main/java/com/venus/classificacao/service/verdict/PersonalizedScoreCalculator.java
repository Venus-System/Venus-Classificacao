package com.venus.classificacao.service.verdict;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.quality.BucketWeights;
import com.venus.classificacao.service.quality.QualityResult;
import com.venus.classificacao.service.quality.QualityScoreCalculator;
import com.venus.classificacao.service.question.ProfileQuestionStrategy;
import com.venus.classificacao.service.question.ProfileResult;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PersonalizedScoreCalculator {

    private final QualityScoreCalculator qualityScoreCalculator;
    private final List<ProfileQuestionStrategy> questionStrategies;
    private final AllergyMatcher allergyMatcher;
    private final FinalScorePolicy finalScorePolicy;

    public PersonalizedScoreCalculator(QualityScoreCalculator qualityScoreCalculator,
            List<ProfileQuestionStrategy> questionStrategies,
            AllergyMatcher allergyMatcher,
            FinalScorePolicy finalScorePolicy) {
        this.qualityScoreCalculator = qualityScoreCalculator;
        this.questionStrategies = questionStrategies;
        this.allergyMatcher = allergyMatcher;
        this.finalScorePolicy = finalScorePolicy;
    }

    public Set<Long> activeTagIds(ProfileSnapshot profile) {
        return questionStrategies.stream()
                .flatMap(strategy -> strategy.activeTagIds(profile).stream())
                .collect(Collectors.toSet());
    }

    public Evaluation calculate(ProductSnapshot product, ProfileSnapshot profile, BucketWeights bucketWeights) {
        QualityResult qualityResult = qualityScoreCalculator.calculate(product, bucketWeights);
        ProfileResult profileResult = scoreProfileQuestions(product, profile);
        CombinedScore combinedScore = CombinedScore.of(qualityResult, profileResult);

        List<AllergyMatch> allergies = allergyMatcher.findMatches(profile, product);
        boolean blockedByRule = profileResult.hasBlock();
        Verdict verdict = finalScorePolicy.decide(combinedScore.total(), blockedByRule, allergies);

        int ingredientCount = product.ingredients().size();
        int unevaluatedIngredientCount = product.unevaluatedIngredientCount();

        return new Evaluation(qualityResult, profileResult, combinedScore, allergies, verdict,
                ingredientCount, unevaluatedIngredientCount);
    }

    private ProfileResult scoreProfileQuestions(ProductSnapshot product, ProfileSnapshot profile) {
        List<QuestionScore> questionScores = questionStrategies.stream()
                .map(strategy -> strategy.score(product, profile))
                .toList();

        return new ProfileResult(questionScores);
    }
}
