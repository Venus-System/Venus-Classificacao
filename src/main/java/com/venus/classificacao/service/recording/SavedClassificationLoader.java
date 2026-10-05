package com.venus.classificacao.service.recording;

import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import com.venus.classificacao.entity.ingredient.Ingredient;
import com.venus.classificacao.entity.scan.AnalysisResult;
import com.venus.classificacao.entity.scan.PersonalizedScore;
import com.venus.classificacao.entity.scan.RuleEvaluation;
import com.venus.classificacao.entity.shared.ProfileTag;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.repository.scan.PersonalizedScoreRepository;
import com.venus.classificacao.repository.scan.RuleEvaluationRepository;
import com.venus.classificacao.service.ClassificationResult;
import com.venus.classificacao.service.ClassificationTarget;
import com.venus.classificacao.service.explanation.ExplanationBuilder;
import com.venus.classificacao.service.explanation.Reason;
import com.venus.classificacao.service.question.MatchedRule;
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
public class SavedClassificationLoader {

    private static final Logger log = LoggerFactory.getLogger(SavedClassificationLoader.class);
    private static final Integer NOT_SAVED = null;
    private static final BigDecimal NOT_SAVED_POINTS = null;

    private final PersonalizedScoreRepository personalizedScoreRepository;
    private final RuleEvaluationRepository ruleEvaluationRepository;
    private final ExplanationBuilder explanationBuilder;

    public SavedClassificationLoader(PersonalizedScoreRepository personalizedScoreRepository,
            RuleEvaluationRepository ruleEvaluationRepository,
            ExplanationBuilder explanationBuilder) {
        this.personalizedScoreRepository = personalizedScoreRepository;
        this.ruleEvaluationRepository = ruleEvaluationRepository;
        this.explanationBuilder = explanationBuilder;
    }

    public Optional<ClassificationResult> load(ClassificationTarget target) {
        Optional<PersonalizedScore> personalizedScore = executeOrFail(() -> personalizedScoreRepository
                        .findByUserIdAndProductVersionIdAndScoringModelId(target.userId(), target.productVersionId(),
                                target.scoringModelId()),
                "Falha ao consultar score personalizado no banco de dados");

        return personalizedScore.map(score -> toSavedResult(target, score));
    }

    private ClassificationResult toSavedResult(ClassificationTarget target, PersonalizedScore personalizedScore) {
        AnalysisResult analysis = personalizedScore.getAnalysisResult();
        ClassificationResult.Breakdown breakdown = savedBreakdownOf(analysis);
        List<Reason> reasons = savedReasonsOf(analysis.getId());
        return new ClassificationResult(
                target.userId(),
                target.productVersionId(),
                target.scoringModelId(),
                personalizedScore.getFinalScore(),
                personalizedScore.getRecommendationLevel(),
                personalizedScore.getRiskLevel(),
                personalizedScore.getCompatibilityPercentage(),
                NOT_SAVED,
                NOT_SAVED,
                breakdown,
                reasons,
                personalizedScore.getSummary(),
                analysis.getCreatedAt(),
                analysis.getProcessingTimeMs());
    }

    private ClassificationResult.Breakdown savedBreakdownOf(AnalysisResult analysis) {
        return new ClassificationResult.Breakdown(
                NOT_SAVED,
                NOT_SAVED_POINTS,
                NOT_SAVED_POINTS,
                analysis.getHealthScore(),
                analysis.getEnvironmentalScore(),
                analysis.getEthicalScore(),
                analysis.getPerformanceScore());
    }

    private List<Reason> savedReasonsOf(Long analysisResultId) {
        List<RuleEvaluation> ruleEvaluations = executeOrFail(
                () -> ruleEvaluationRepository.findByAnalysisResultId(analysisResultId),
                "Falha ao consultar avaliacoes de regra da analise");

        List<MatchedRule> matchedRules = ruleEvaluations.stream()
                .map(this::toMatchedRule)
                .toList();
        return explanationBuilder.ruleReasonsOf(matchedRules);
    }

    private MatchedRule toMatchedRule(RuleEvaluation ruleEvaluation) {
        CompatibilityRule rule = ruleEvaluation.getCompatibilityRule();
        Ingredient ingredient = ruleEvaluation.getIngredient();
        ProfileTag profileTag = ruleEvaluation.getProfileTag();
        return new MatchedRule(
                rule.getId(),
                ingredient.getId(),
                ingredient.getCommonName(),
                profileTag.getId(),
                profileTag.getSlug(),
                rule.getEffectType(),
                ruleEvaluation.getScoreDelta().intValue(),
                rule.getWeight(),
                ruleEvaluation.getFinalDelta(),
                ruleEvaluation.getExplanation());
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
