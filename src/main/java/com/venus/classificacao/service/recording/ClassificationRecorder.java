package com.venus.classificacao.service.recording;

import com.venus.classificacao.entity.enums.AnalysisStatus;
import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import com.venus.classificacao.entity.ingredient.Ingredient;
import com.venus.classificacao.entity.product.ProductVersion;
import com.venus.classificacao.entity.scan.AnalysisResult;
import com.venus.classificacao.entity.scan.PersonalizedScore;
import com.venus.classificacao.entity.scan.RuleEvaluation;
import com.venus.classificacao.entity.scoring.ProductScore;
import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.entity.shared.ProfileTag;
import com.venus.classificacao.entity.user.User;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.repository.scan.AnalysisResultRepository;
import com.venus.classificacao.repository.scan.PersonalizedScoreRepository;
import com.venus.classificacao.repository.scan.RuleEvaluationRepository;
import com.venus.classificacao.repository.scoring.ProductScoreRepository;
import com.venus.classificacao.service.ClassificationResult;
import com.venus.classificacao.service.question.MatchedRule;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ClassificationRecorder {

    private static final Logger log = LoggerFactory.getLogger(ClassificationRecorder.class);

    private final EntityManager entityManager;
    private final AnalysisResultRepository analysisResultRepository;
    private final PersonalizedScoreRepository personalizedScoreRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final ProductScoreRepository productScoreRepository;

    public ClassificationRecorder(EntityManager entityManager,
            AnalysisResultRepository analysisResultRepository,
            PersonalizedScoreRepository personalizedScoreRepository,
            RuleEvaluationRepository ruleEvaluationRepository,
            ProductScoreRepository productScoreRepository) {
        this.entityManager = entityManager;
        this.analysisResultRepository = analysisResultRepository;
        this.personalizedScoreRepository = personalizedScoreRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.productScoreRepository = productScoreRepository;
    }

    public void record(ClassificationResult result, List<MatchedRule> matchedRules) {
        User user = entityManager.getReference(User.class, result.userId());
        ProductVersion version = entityManager.getReference(ProductVersion.class, result.productVersionId());
        ScoringModel model = entityManager.getReference(ScoringModel.class, result.scoringModelId());

        AnalysisResult newAnalysis = toAnalysisResult(result, user, version, model);
        AnalysisResult analysis = executeOrFail(() -> analysisResultRepository.save(newAnalysis),
                "Falha ao criar analise no banco de dados");

        PersonalizedScore personalizedScore = updatedPersonalizedScore(result, analysis, user, version, model);
        executeOrFail(() -> personalizedScoreRepository.save(personalizedScore),
                "Falha ao atualizar score personalizado no banco de dados");

        List<RuleEvaluation> ruleEvaluations = matchedRules.stream()
                .map(rule -> toRuleEvaluation(rule, analysis))
                .toList();
        executeOrFail(() -> ruleEvaluationRepository.saveAll(ruleEvaluations),
                "Falha ao criar avaliacao de regra no banco de dados");

        ProductScore productScore = updatedProductScore(result, version, model);
        executeOrFail(() -> productScoreRepository.save(productScore),
                "Falha ao atualizar score de produto no banco de dados");

        executeOrFail(() -> {
            entityManager.flush();
            return null;
        }, "Falha ao gravar a classificacao no banco de dados");
    }

    private AnalysisResult toAnalysisResult(ClassificationResult result, User user, ProductVersion version,
            ScoringModel model) {
        ClassificationResult.Breakdown breakdown = result.breakdown();
        AnalysisResult analysis = new AnalysisResult();
        analysis.setUser(user);
        analysis.setProductVersion(version);
        analysis.setScoringModel(model);
        analysis.setOverallScore(result.finalScore());
        analysis.setHealthScore(breakdown.healthScore());
        analysis.setEnvironmentalScore(breakdown.environmentalScore());
        analysis.setEthicalScore(breakdown.ethicalScore());
        analysis.setPerformanceScore(breakdown.performanceScore());
        analysis.setTransparencyScore(null);
        analysis.setConfidenceScore(null);
        analysis.setProcessingTimeMs(result.processingTimeMs());
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setSummary(result.summary());
        return analysis;
    }

    private PersonalizedScore updatedPersonalizedScore(ClassificationResult result, AnalysisResult analysis, User user,
            ProductVersion version, ScoringModel model) {
        Optional<PersonalizedScore> currentPersonalizedScore = executeOrFail(() -> personalizedScoreRepository
                        .findByUserIdAndProductVersionIdAndScoringModelId(result.userId(), result.productVersionId(),
                                result.scoringModelId()),
                "Falha ao verificar score personalizado existente");

        PersonalizedScore personalizedScore = currentPersonalizedScore
                .orElseGet(() -> newPersonalizedScore(user, version, model));
        personalizedScore.setAnalysisResult(analysis);
        personalizedScore.setFinalScore(result.finalScore());
        personalizedScore.setCompatibilityPercentage(result.compatibilityPercentage());
        personalizedScore.setRiskLevel(result.riskLevel());
        personalizedScore.setRecommendationLevel(result.recommendationLevel());
        personalizedScore.setSummary(result.summary());
        return personalizedScore;
    }

    private PersonalizedScore newPersonalizedScore(User user, ProductVersion version, ScoringModel model) {
        PersonalizedScore personalizedScore = new PersonalizedScore();
        personalizedScore.setUser(user);
        personalizedScore.setProductVersion(version);
        personalizedScore.setScoringModel(model);
        return personalizedScore;
    }

    private RuleEvaluation toRuleEvaluation(MatchedRule rule, AnalysisResult analysis) {
        RuleEvaluation evaluation = new RuleEvaluation();
        evaluation.setAnalysisResult(analysis);
        evaluation.setCompatibilityRule(entityManager.getReference(CompatibilityRule.class, rule.ruleId()));
        evaluation.setIngredient(entityManager.getReference(Ingredient.class, rule.ingredientId()));
        evaluation.setProfileTag(entityManager.getReference(ProfileTag.class, rule.profileTagId()));
        evaluation.setWasMatched(true);
        evaluation.setScoreDelta(BigDecimal.valueOf(rule.scoreDelta()));
        evaluation.setFinalDelta(rule.impact());
        evaluation.setExplanation(rule.explanationText());
        return evaluation;
    }

    private ProductScore updatedProductScore(ClassificationResult result, ProductVersion version, ScoringModel model) {
        Optional<ProductScore> currentProductScore = executeOrFail(() -> productScoreRepository
                        .findByProductVersionIdAndScoringModelId(result.productVersionId(), result.scoringModelId()),
                "Falha ao consultar score de produto no banco de dados");

        ProductScore productScore = currentProductScore.orElseGet(() -> newProductScore(version, model));
        ClassificationResult.Breakdown breakdown = result.breakdown();
        productScore.setOverallScore(breakdown.qualityScore());
        productScore.setHealthScore(breakdown.healthScore());
        productScore.setEnvironmentalScore(breakdown.environmentalScore());
        productScore.setEthicalScore(breakdown.ethicalScore());
        productScore.setPerformanceScore(breakdown.performanceScore());
        productScore.setTransparencyScore(null);
        productScore.setConfidenceScore(null);
        productScore.setCalculatedAt(result.calculatedAt());
        return productScore;
    }

    private ProductScore newProductScore(ProductVersion version, ScoringModel model) {
        ProductScore productScore = new ProductScore();
        productScore.setProductVersion(version);
        productScore.setScoringModel(model);
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
