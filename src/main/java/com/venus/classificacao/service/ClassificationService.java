package com.venus.classificacao.service;

import com.venus.classificacao.dto.request.ClassificationRequest;
import com.venus.classificacao.dto.response.ClassificationResponse;
import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.mapper.ClassificationMapper;
import com.venus.classificacao.service.explanation.Explanation;
import com.venus.classificacao.service.explanation.ExplanationBuilder;
import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.product.ProductSnapshotLoader;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshotLoader;
import com.venus.classificacao.service.quality.BucketWeights;
import com.venus.classificacao.service.quality.BucketWeightsLoader;
import com.venus.classificacao.service.recording.ClassificationRecorder;
import com.venus.classificacao.service.scoring.ScoringModelLoader;
import com.venus.classificacao.service.verdict.Evaluation;
import com.venus.classificacao.service.verdict.PersonalizedScoreCalculator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassificationService {

    private final ScoringModelLoader scoringModelLoader;
    private final ProfileSnapshotLoader profileSnapshotLoader;
    private final ProductSnapshotLoader productSnapshotLoader;
    private final BucketWeightsLoader bucketWeightsLoader;
    private final PersonalizedScoreCalculator personalizedScoreCalculator;
    private final ExplanationBuilder explanationBuilder;
    private final ClassificationRecorder classificationRecorder;
    private final ClassificationMapper classificationMapper;

    public ClassificationService(ScoringModelLoader scoringModelLoader,
            ProfileSnapshotLoader profileSnapshotLoader,
            ProductSnapshotLoader productSnapshotLoader,
            BucketWeightsLoader bucketWeightsLoader,
            PersonalizedScoreCalculator personalizedScoreCalculator,
            ExplanationBuilder explanationBuilder,
            ClassificationRecorder classificationRecorder,
            ClassificationMapper classificationMapper) {
        this.scoringModelLoader = scoringModelLoader;
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
        ClassificationResult result = classifyAndRecord(request.userId(), request.productVersionId(),
                request.scoringModelId());

        return classificationMapper.toResponse(result);
    }

    private ClassificationResult classifyAndRecord(Long userId, Long productVersionId, Long scoringModelId) {
        long startedAt = System.nanoTime();

        ScoringModel scoringModel = scoringModelLoader.load(scoringModelId);
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

    private int elapsedMillis(long startedAt) {
        return Math.toIntExact(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }
}
