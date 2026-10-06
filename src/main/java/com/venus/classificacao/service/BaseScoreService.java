package com.venus.classificacao.service;

import com.venus.classificacao.dto.response.BaseScoreResponse;
import com.venus.classificacao.dto.response.BaseScoreSummaryResponse;
import com.venus.classificacao.entity.product.ProductVersion;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.exception.UnprocessableAnalysisException;
import com.venus.classificacao.mapper.BaseScoreMapper;
import com.venus.classificacao.repository.product.ProductVersionRepository;
import com.venus.classificacao.service.basescore.BaseScoreRecalculator;
import com.venus.classificacao.service.basescore.BaseScoreResult;
import com.venus.classificacao.service.basescore.BaseScoreSummary;
import com.venus.classificacao.service.basescore.BaseScoreSummary.SkippedVersion;
import com.venus.classificacao.service.quality.BucketWeights;
import com.venus.classificacao.service.quality.BucketWeightsLoader;
import com.venus.classificacao.service.scoring.ScoringModelLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class BaseScoreService {

    private static final Logger log = LoggerFactory.getLogger(BaseScoreService.class);

    private static final int VERSIONS_PER_BATCH = 100;

    private final ScoringModelLoader scoringModelLoader;
    private final BucketWeightsLoader bucketWeightsLoader;
    private final BaseScoreRecalculator baseScoreRecalculator;
    private final ProductVersionRepository productVersionRepository;
    private final BaseScoreMapper baseScoreMapper;

    public BaseScoreService(ScoringModelLoader scoringModelLoader,
            BucketWeightsLoader bucketWeightsLoader,
            BaseScoreRecalculator baseScoreRecalculator,
            ProductVersionRepository productVersionRepository,
            BaseScoreMapper baseScoreMapper) {
        this.scoringModelLoader = scoringModelLoader;
        this.bucketWeightsLoader = bucketWeightsLoader;
        this.baseScoreRecalculator = baseScoreRecalculator;
        this.productVersionRepository = productVersionRepository;
        this.baseScoreMapper = baseScoreMapper;
    }

    public BaseScoreResponse recalculateVersion(Long productVersionId, Long scoringModelId) {
        Long modelId = scoringModelLoader.load(scoringModelId).getId();
        BucketWeights bucketWeights = bucketWeightsLoader.load(modelId);

        BaseScoreResult result = baseScoreRecalculator.recalculate(productVersionId, modelId, bucketWeights);
        return baseScoreMapper.toResponse(result);
    }

    public BaseScoreSummaryResponse recalculateAll(Long scoringModelId) {
        long startedAt = System.nanoTime();
        Long modelId = scoringModelLoader.load(scoringModelId).getId();
        BucketWeights bucketWeights = bucketWeightsLoader.load(modelId);

        List<SkippedVersion> skipped = new ArrayList<>();
        int totalVersions = forEachCurrentVersion(
                version -> recalculateOrSkip(version, modelId, bucketWeights).ifPresent(skipped::add));

        BaseScoreSummary summary = new BaseScoreSummary(modelId, totalVersions, totalVersions - skipped.size(), skipped,
                elapsedMillis(startedAt));
        return baseScoreMapper.toSummaryResponse(summary);
    }

    private int forEachCurrentVersion(Consumer<ProductVersion> action) {
        int visitedVersions = 0;
        Pageable batch = PageRequest.of(0, VERSIONS_PER_BATCH);
        Slice<ProductVersion> currentVersions;
        do {
            currentVersions = currentVersionsOf(batch);
            currentVersions.forEach(action);
            visitedVersions += currentVersions.getNumberOfElements();
            batch = currentVersions.nextPageable();
        } while (currentVersions.hasNext());
        return visitedVersions;
    }

    private Slice<ProductVersion> currentVersionsOf(Pageable batch) {
        return executeOrFail(() -> productVersionRepository.findByIsCurrentTrueOrderByIdAsc(batch),
                "Falha ao consultar versoes atuais dos produtos");
    }

    private Optional<SkippedVersion> recalculateOrSkip(ProductVersion version, Long modelId, BucketWeights bucketWeights) {
        try {
            baseScoreRecalculator.recalculate(version.getId(), modelId, bucketWeights);
            return Optional.empty();
        } catch (UnprocessableAnalysisException ex) {
            return Optional.of(new SkippedVersion(version.getProduct().getId(), version.getId(), ex.getCode()));
        }
    }

    private int elapsedMillis(long startedAt) {
        return Math.toIntExact(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
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
