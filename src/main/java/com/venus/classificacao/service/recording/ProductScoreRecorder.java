package com.venus.classificacao.service.recording;

import com.venus.classificacao.entity.product.ProductVersion;
import com.venus.classificacao.entity.scoring.ProductScore;
import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.repository.scoring.ProductScoreRepository;
import com.venus.classificacao.service.quality.QualityScores;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ProductScoreRecorder {

    private static final Logger log = LoggerFactory.getLogger(ProductScoreRecorder.class);

    private final EntityManager entityManager;
    private final ProductScoreRepository productScoreRepository;

    public ProductScoreRecorder(EntityManager entityManager, ProductScoreRepository productScoreRepository) {
        this.entityManager = entityManager;
        this.productScoreRepository = productScoreRepository;
    }

    public void record(Long productVersionId, Long scoringModelId, QualityScores scores, OffsetDateTime calculatedAt) {
        ProductScore productScore = currentOrNewProductScore(productVersionId, scoringModelId);
        productScore.setOverallScore(scores.qualityScore());
        productScore.setHealthScore(scores.healthScore());
        productScore.setEnvironmentalScore(scores.environmentalScore());
        productScore.setEthicalScore(scores.ethicalScore());
        productScore.setPerformanceScore(scores.performanceScore());
        productScore.setTransparencyScore(null);
        productScore.setConfidenceScore(null);
        productScore.setCalculatedAt(calculatedAt);

        executeOrFail(() -> productScoreRepository.save(productScore),
                "Falha ao atualizar score de produto no banco de dados");
    }

    private ProductScore currentOrNewProductScore(Long productVersionId, Long scoringModelId) {
        Optional<ProductScore> currentProductScore = executeOrFail(() -> productScoreRepository
                        .findByProductVersionIdAndScoringModelId(productVersionId, scoringModelId),
                "Falha ao consultar score de produto no banco de dados");

        return currentProductScore.orElseGet(() -> newProductScore(productVersionId, scoringModelId));
    }

    private ProductScore newProductScore(Long productVersionId, Long scoringModelId) {
        ProductScore productScore = new ProductScore();
        productScore.setProductVersion(entityManager.getReference(ProductVersion.class, productVersionId));
        productScore.setScoringModel(entityManager.getReference(ScoringModel.class, scoringModelId));
        return productScore;
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
