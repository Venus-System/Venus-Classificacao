package com.venus.classificacao.service.quality.strategy;

import static com.venus.classificacao.service.product.ProductSnapshot.IngredientData.MAX_NOTE;
import static com.venus.classificacao.service.quality.ScoreScale.FULL_SCORE;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.ScoreBucket;
import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

@Component
public class HealthScoreStrategy implements BucketScoreStrategy {

    @Override
    public ScoreBucket bucket() {
        return ScoreBucket.HEALTH;
    }

    @Override
    public OptionalDouble score(ProductSnapshot product) {
        OptionalDouble averageRisk = product.evaluatedIngredients().stream()
                .mapToDouble(this::riskNoteOf)
                .average();

        if (averageRisk.isEmpty()) {
            return OptionalDouble.empty();
        }

        double safetyShare = 1 - averageRisk.getAsDouble() / MAX_NOTE;
        return OptionalDouble.of(FULL_SCORE * safetyShare);
    }

    private double riskNoteOf(ProductSnapshot.IngredientData ingredient) {
        return (ingredient.irritationRisk() + ingredient.comedogenicity()) / 2.0;
    }
}
