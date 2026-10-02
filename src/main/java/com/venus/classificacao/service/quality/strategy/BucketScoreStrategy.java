package com.venus.classificacao.service.quality.strategy;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.quality.ScoreBucket;
import java.util.OptionalDouble;

public interface BucketScoreStrategy {

    ScoreBucket bucket();

    OptionalDouble score(ProductSnapshot product);
}
