package com.venus.classificacao.service.quality.strategy;

import static com.venus.classificacao.service.product.ProductSnapshot.IngredientData.MAX_NOTE;
import static com.venus.classificacao.service.quality.ScoreScale.FULL_SCORE;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.ScoreBucket;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentalScoreStrategy implements BucketScoreStrategy {

    private static final double INGREDIENT_SHARE = 0.7;
    private static final double PACKAGING_SHARE = 0.3;
    private static final double POINTS_PER_FEATURE = 25;
    private static final double POINTS_PER_RECYCLED_PERCENT = 0.25;
    private static final double MAX_RECYCLED_PERCENT = 100;

    @Override
    public ScoreBucket bucket() {
        return ScoreBucket.ENVIRONMENTAL;
    }

    @Override
    public OptionalDouble score(ProductSnapshot product) {
        OptionalDouble ingredientScore = ingredientScore(product);
        OptionalDouble packagingScore = packagingScore(product);

        if (ingredientScore.isPresent() && packagingScore.isPresent()) {
            double ingredientPart = ingredientScore.getAsDouble() * INGREDIENT_SHARE;
            double packagingPart = packagingScore.getAsDouble() * PACKAGING_SHARE;
            return OptionalDouble.of(ingredientPart + packagingPart);
        }

        if (ingredientScore.isPresent()) {
            return ingredientScore;
        }
        return packagingScore;
    }

    private OptionalDouble ingredientScore(ProductSnapshot product) {
        OptionalDouble averageEcoNote = product.evaluatedIngredients().stream()
                .mapToDouble(this::ecoNoteOf)
                .average();

        if (averageEcoNote.isEmpty()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(FULL_SCORE * averageEcoNote.getAsDouble() / MAX_NOTE);
    }

    private double ecoNoteOf(ProductSnapshot.IngredientData ingredient) {
        int invertedRiskNote = MAX_NOTE - ingredient.environmentalRisk();
        return (ingredient.biodegradability() + invertedRiskNote) / 2.0;
    }

    private OptionalDouble packagingScore(ProductSnapshot product) {
        Optional<ProductSnapshot.PackagingData> packaging = product.evaluatedPackaging();
        if (packaging.isEmpty()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(packagingPointsOf(packaging.get()));
    }

    private double packagingPointsOf(ProductSnapshot.PackagingData packaging) {
        double featurePoints = 0;
        if (packaging.recyclable()) {
            featurePoints += POINTS_PER_FEATURE;
        }
        if (packaging.refillable()) {
            featurePoints += POINTS_PER_FEATURE;
        }
        if (packaging.biodegradable()) {
            featurePoints += POINTS_PER_FEATURE;
        }

        double recycledPercent = recycledPercentOf(packaging.recycledContentPercentage());
        double recycledContentPoints = POINTS_PER_RECYCLED_PERCENT * recycledPercent;

        return featurePoints + recycledContentPoints;
    }

    private double recycledPercentOf(BigDecimal recycledContentPercentage) {
        if (recycledContentPercentage == null) {
            return 0;
        }

        return Math.max(0, Math.min(MAX_RECYCLED_PERCENT, recycledContentPercentage.doubleValue()));
    }
}
