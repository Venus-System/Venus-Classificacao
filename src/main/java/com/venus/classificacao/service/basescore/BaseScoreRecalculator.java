package com.venus.classificacao.service.basescore;

import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.product.ProductSnapshotLoader;
import com.venus.classificacao.service.quality.BucketWeights;
import com.venus.classificacao.service.quality.QualityScoreCalculator;
import com.venus.classificacao.service.quality.QualityScores;
import com.venus.classificacao.service.recording.ProductScoreRecorder;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BaseScoreRecalculator {

    private static final Logger log = LoggerFactory.getLogger(BaseScoreRecalculator.class);

    private static final Set<Long> NO_PROFILE_TAGS = Set.of();

    private final ProductSnapshotLoader productSnapshotLoader;
    private final QualityScoreCalculator qualityScoreCalculator;
    private final ProductScoreRecorder productScoreRecorder;
    private final EntityManager entityManager;

    public BaseScoreRecalculator(ProductSnapshotLoader productSnapshotLoader,
            QualityScoreCalculator qualityScoreCalculator,
            ProductScoreRecorder productScoreRecorder,
            EntityManager entityManager) {
        this.productSnapshotLoader = productSnapshotLoader;
        this.qualityScoreCalculator = qualityScoreCalculator;
        this.productScoreRecorder = productScoreRecorder;
        this.entityManager = entityManager;
    }

    @Transactional
    public BaseScoreResult recalculate(Long productVersionId, Long scoringModelId, BucketWeights bucketWeights) {
        ProductSnapshot product = productSnapshotLoader.load(productVersionId, scoringModelId, NO_PROFILE_TAGS);
        QualityScores scores = QualityScores.of(qualityScoreCalculator.calculate(product, bucketWeights));
        OffsetDateTime calculatedAt = OffsetDateTime.now();

        productScoreRecorder.record(productVersionId, scoringModelId, scores, calculatedAt);
        flushOrFail();

        return new BaseScoreResult(productVersionId, scoringModelId, scores, product.ingredients().size(),
                product.unevaluatedIngredientCount(), calculatedAt);
    }

    private void flushOrFail() {
        executeOrFail(() -> {
            entityManager.flush();
            return null;
        }, "Falha ao gravar a nota base no banco de dados");
    }

    private <T> T executeOrFail(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException ex) {
            throw DataIntegrityViolationTranslator.translate(ex);
        } catch (DataAccessException ex) {
            log.error(errorMessage, ex);
            throw DataAccessFailureTranslator.translate(ex, errorMessage);
        }
    }
}
