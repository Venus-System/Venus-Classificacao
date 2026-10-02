package com.venus.classificacao.service;

import com.venus.classificacao.dto.request.ClassificationRequest;
import com.venus.classificacao.dto.response.ClassificationResponse;
import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.exception.ResourceNotFoundException;
import com.venus.classificacao.mapper.ClassificationMapper;
import com.venus.classificacao.repository.scoring.ScoringModelRepository;
import com.venus.classificacao.service.explanation.Explanation;
import com.venus.classificacao.service.explanation.ExplanationBuilder;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.product.ProductSnapshotLoader;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshotLoader;
import com.venus.classificacao.service.quality.BucketWeights;
import com.venus.classificacao.service.quality.BucketWeightsLoader;
import com.venus.classificacao.service.recording.ClassificationRecorder;
import com.venus.classificacao.service.verdict.Evaluation;
import com.venus.classificacao.service.verdict.PersonalizedScoreCalculator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    private final ScoringModelRepository scoringModelRepository;
    private final ProfileSnapshotLoader profileSnapshotLoader;
    private final ProductSnapshotLoader productSnapshotLoader;
    private final BucketWeightsLoader bucketWeightsLoader;
    private final PersonalizedScoreCalculator personalizedScoreCalculator;
    private final ExplanationBuilder explanationBuilder;
    private final ClassificationRecorder classificationRecorder;
    private final ClassificationMapper classificationMapper;

    public ClassificationService(ScoringModelRepository scoringModelRepository,
            ProfileSnapshotLoader profileSnapshotLoader,
            ProductSnapshotLoader productSnapshotLoader,
            BucketWeightsLoader bucketWeightsLoader,
            PersonalizedScoreCalculator personalizedScoreCalculator,
            ExplanationBuilder explanationBuilder,
            ClassificationRecorder classificationRecorder,
            ClassificationMapper classificationMapper) {
        this.scoringModelRepository = scoringModelRepository;
        this.profileSnapshotLoader = profileSnapshotLoader;
        this.productSnapshotLoader = productSnapshotLoader;
        this.bucketWeightsLoader = bucketWeightsLoader;
        this.personalizedScoreCalculator = personalizedScoreCalculator;
        this.explanationBuilder = explanationBuilder;
        this.classificationRecorder = classificationRecorder;
        this.classificationMapper = classificationMapper;
    }

    @Transactional
    public ClassificationResponse classify(ClassificationRequest request) {
        ClassificationResult result = executeOrFail(
                () -> classifyAndRecord(request.userId(), request.productVersionId(), request.scoringModelId()),
                "Falha ao classificar o produto no banco de dados");

        return classificationMapper.toResponse(result);
    }

    private ClassificationResult classifyAndRecord(Long userId, Long productVersionId, Long scoringModelId) {
        long startedAt = System.nanoTime();

        ScoringModel scoringModel = requestedOrActiveModel(scoringModelId);
        ProfileSnapshot profile = profileSnapshotLoader.load(userId);
        Set<Long> activeTagIds = personalizedScoreCalculator.activeTagIds(profile);
        ProductSnapshot product = productSnapshotLoader.load(productVersionId, scoringModel.getId(), activeTagIds);
        BucketWeights bucketWeights = bucketWeightsLoader.load(scoringModel.getId());

        Evaluation evaluation = personalizedScoreCalculator.calculate(product, profile, bucketWeights);
        Explanation explanation = explanationBuilder.build(evaluation);

        ClassificationTarget target = new ClassificationTarget(userId, productVersionId, scoringModel.getId());
        int processingTimeMs = elapsedMillis(startedAt);
        ClassificationResult result = ClassificationResult.of(target, evaluation, explanation, processingTimeMs);

        classificationRecorder.record(result, evaluation.profileResult().distinctMatchedRules());
        return result;
    }

    private ScoringModel requestedOrActiveModel(Long scoringModelId) {
        if (scoringModelId == null) {
            return scoringModelRepository.findFirstByIsActiveTrueOrderByIdDesc()
                    .orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.NO_ACTIVE_SCORING_MODEL,
                            "Nenhum modelo de score ativo"));
        }
        return scoringModelRepository.findById(scoringModelId)
                .orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.SCORING_MODEL_NOT_FOUND,
                        "Modelo de score nao encontrado com id " + scoringModelId));
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
