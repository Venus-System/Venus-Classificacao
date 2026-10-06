package com.venus.classificacao.service.quality.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.venus.classificacao.service.product.ProductSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BucketScoreStrategiesTest {

    private static final double PRECISION = 1e-9;

    private final HealthScoreStrategy health = new HealthScoreStrategy();
    private final EnvironmentalScoreStrategy environmental = new EnvironmentalScoreStrategy();
    private final EthicalScoreStrategy ethical = new EthicalScoreStrategy();
    private final PerformanceScoreStrategy performance = new PerformanceScoreStrategy();

    @Test
    void healthIsTheSafetyShareOfTheEvaluatedIngredients() {
        ProductSnapshot product = product(List.of(evaluated(1L, 2, 4, 0, 0), evaluated(2L, 0, 2, 0, 0), unevaluated(3L)),
                Optional.empty(), Map.of());

        assertThat(health.score(product).getAsDouble()).isCloseTo(80.0, within(PRECISION));
    }

    @Test
    void healthIsEmptyWhenNoIngredientIsEvaluated() {
        ProductSnapshot product = product(List.of(unevaluated(1L)), Optional.empty(), Map.of());

        assertThat(health.score(product)).isEmpty();
    }

    @Test
    void environmentalMixesSeventyPercentIngredientsAndThirtyPercentPackaging() {
        ProductSnapshot product = product(List.of(evaluated(1L, 0, 0, 8, 2)),
                Optional.of(evaluatedPackaging(true, true, false, "40")), Map.of());

        assertThat(environmental.score(product).getAsDouble()).isCloseTo(74.0, within(PRECISION));
    }

    @Test
    void environmentalUsesOnlyThePackagingWhenNoIngredientIsEvaluated() {
        ProductSnapshot product = product(List.of(unevaluated(1L)),
                Optional.of(evaluatedPackaging(true, true, false, "40")), Map.of());

        assertThat(environmental.score(product).getAsDouble()).isCloseTo(60.0, within(PRECISION));
    }

    @Test
    void environmentalUsesOnlyTheIngredientsWhenThePackagingIsNotEvaluated() {
        ProductSnapshot product = product(List.of(evaluated(1L, 0, 0, 8, 2)),
                Optional.of(unevaluatedPackaging(true, true, false, "40")), Map.of());

        assertThat(environmental.score(product).getAsDouble()).isCloseTo(80.0, within(PRECISION));
    }

    @Test
    void environmentalIsEmptyWithoutEvaluatedIngredientAndWithUnevaluatedPackaging() {
        ProductSnapshot product = product(List.of(unevaluated(1L)),
                Optional.of(unevaluatedPackaging(false, false, false, "0")), Map.of());

        assertThat(environmental.score(product)).isEmpty();
    }

    @Test
    void environmentalIsEmptyWithoutEvaluatedIngredientAndWithoutPackaging() {
        ProductSnapshot product = product(List.of(unevaluated(1L)), Optional.empty(), Map.of());

        assertThat(environmental.score(product)).isEmpty();
    }

    @Test
    void ethicalStartsAtFiftyAndCapsTheSealsAtTenPoints() {
        ProductSnapshot product = new ProductSnapshot(1L, new ProductSnapshot.BrandClaims(true, true),
                List.of(unevaluated(1L)), Optional.empty(), 3, Map.of(), List.of());

        assertThat(ethical.score(product).getAsDouble()).isEqualTo(100.0);
    }

    @Test
    void performanceCountsAtMostThreeBenefitsPerEvaluatedIngredient() {
        ProductSnapshot product = product(List.of(evaluated(1L, 0, 0, 0, 0), evaluated(2L, 0, 0, 0, 0), unevaluated(3L)),
                Optional.empty(), Map.of(1L, 1, 2L, 5, 3L, 3));

        assertThat(performance.score(product).getAsDouble()).isCloseTo(200.0 / 3, within(PRECISION));
    }

    @Test
    void performanceIsEmptyWhenNoIngredientIsEvaluated() {
        ProductSnapshot product = product(List.of(unevaluated(1L)), Optional.empty(), Map.of(1L, 2));

        assertThat(performance.score(product)).isEmpty();
    }

    private static ProductSnapshot product(List<ProductSnapshot.IngredientData> ingredients,
            Optional<ProductSnapshot.PackagingData> packaging, Map<Long, Integer> benefitCountByIngredientId) {
        return new ProductSnapshot(1L, new ProductSnapshot.BrandClaims(false, false), ingredients, packaging, 0,
                benefitCountByIngredientId, List.of());
    }

    private static ProductSnapshot.IngredientData evaluated(Long id, int irritationRisk, int comedogenicity,
            int biodegradability, int environmentalRisk) {
        return new ProductSnapshot.IngredientData(id, "Ingrediente " + id, irritationRisk, comedogenicity,
                biodegradability, environmentalRisk, true);
    }

    private static ProductSnapshot.IngredientData unevaluated(Long id) {
        return new ProductSnapshot.IngredientData(id, "Ingrediente " + id, 0, 0, 0, 0, false);
    }

    private static ProductSnapshot.PackagingData evaluatedPackaging(boolean recyclable, boolean refillable,
            boolean biodegradable, String recycledContentPercentage) {
        return packaging(recyclable, refillable, biodegradable, recycledContentPercentage, true);
    }

    private static ProductSnapshot.PackagingData unevaluatedPackaging(boolean recyclable, boolean refillable,
            boolean biodegradable, String recycledContentPercentage) {
        return packaging(recyclable, refillable, biodegradable, recycledContentPercentage, false);
    }

    private static ProductSnapshot.PackagingData packaging(boolean recyclable, boolean refillable,
            boolean biodegradable, String recycledContentPercentage, boolean evaluated) {
        return new ProductSnapshot.PackagingData(recyclable, refillable, biodegradable,
                new BigDecimal(recycledContentPercentage), evaluated);
    }
}
