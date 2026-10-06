package com.venus.classificacao.service.quality.strategy;

import static com.venus.classificacao.service.quality.ScoreScale.FULL_SCORE;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.ScoreBucket;
import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

@Component
public class PerformanceScoreStrategy implements BucketScoreStrategy {

    private static final int MAX_BENEFITS_PER_INGREDIENT = 3;

    @Override
    public ScoreBucket bucket() {
        return ScoreBucket.PERFORMANCE;
    }

    @Override
    public OptionalDouble score(ProductSnapshot product) {
        OptionalDouble averageBenefitCount = product.evaluatedIngredients().stream()
                .mapToInt(ingredient -> cappedBenefitCountOf(product, ingredient))
                .average();

        if (averageBenefitCount.isEmpty()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(FULL_SCORE * averageBenefitCount.getAsDouble() / MAX_BENEFITS_PER_INGREDIENT);
    }

    private int cappedBenefitCountOf(ProductSnapshot product, ProductSnapshot.IngredientData ingredient) {
        return Math.min(product.benefitCountOf(ingredient.id()), MAX_BENEFITS_PER_INGREDIENT);
    }
}
