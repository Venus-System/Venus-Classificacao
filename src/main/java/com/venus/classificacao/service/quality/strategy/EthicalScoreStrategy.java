package com.venus.classificacao.service.quality.strategy;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.ScoreBucket;
import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

@Component
public class EthicalScoreStrategy implements BucketScoreStrategy {

    private static final double NEUTRAL_SCORE = 50;
    private static final double VEGAN_POINTS = 20;
    private static final double CRUELTY_FREE_POINTS = 20;
    private static final double POINTS_PER_SEAL = 5;
    private static final double MAX_SEAL_POINTS = 10;

    @Override
    public ScoreBucket bucket() {
        return ScoreBucket.ETHICAL;
    }

    @Override
    public OptionalDouble score(ProductSnapshot product) {
        ProductSnapshot.BrandClaims brandClaims = product.brandClaims();

        double veganPoints = brandClaims.vegan() ? VEGAN_POINTS : 0;
        double crueltyFreePoints = brandClaims.crueltyFree() ? CRUELTY_FREE_POINTS : 0;
        double sealPoints = Math.min(POINTS_PER_SEAL * product.verifiedEthicalSealCount(), MAX_SEAL_POINTS);

        return OptionalDouble.of(NEUTRAL_SCORE + veganPoints + crueltyFreePoints + sealPoints);
    }
}
