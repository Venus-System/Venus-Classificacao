package com.venus.classificacao.service.explanation;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import com.venus.classificacao.service.question.MatchedRule;
import com.venus.classificacao.service.verdict.AllergyMatch;
import com.venus.classificacao.service.verdict.Evaluation;
import com.venus.classificacao.service.verdict.Verdict;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class ExplanationBuilder {

    private static final int MAX_REASONS_IN_SUMMARY = 2;
    private static final String WARNING_PREFIX = "Atenção: ";

    public Explanation build(Evaluation evaluation) {
        List<Reason> reasons = reasonsOf(evaluation);
        String summary = summaryOf(evaluation.verdict(), reasons);

        return new Explanation(reasons, summary);
    }

    public List<Reason> ruleReasonsOf(List<MatchedRule> matchedRules) {
        return Stream.of(blockReasons(matchedRules), ruleReasons(matchedRules))
                .flatMap(List::stream)
                .toList();
    }

    private List<Reason> reasonsOf(Evaluation evaluation) {
        List<MatchedRule> matchedRules = evaluation.profileResult().distinctMatchedRules();
        List<String> preferenceNotes = evaluation.profileResult().preferenceNotes();

        return Stream.of(
                        blockReasons(matchedRules),
                        allergyReasons(evaluation.allergies()),
                        preferenceReasons(preferenceNotes),
                        ruleReasons(matchedRules),
                        baseScoreReasons(evaluation))
                .flatMap(List::stream)
                .toList();
    }

    private List<Reason> blockReasons(List<MatchedRule> matchedRules) {
        return matchedRules.stream()
                .filter(MatchedRule::isBlock)
                .map(Reason::ofRule)
                .toList();
    }

    private List<Reason> allergyReasons(List<AllergyMatch> allergies) {
        return allergies.stream()
                .map(match -> Reason.ofAllergy(allergyText(match)))
                .toList();
    }

    private List<Reason> preferenceReasons(List<String> preferenceNotes) {
        return preferenceNotes.stream()
                .map(Reason::ofPreference)
                .toList();
    }

    private List<Reason> ruleReasons(List<MatchedRule> matchedRules) {
        return matchedRules.stream()
                .filter(rule -> !rule.isBlock())
                .sorted(Comparator.comparing((MatchedRule rule) -> rule.impact().abs()).reversed())
                .map(Reason::ofRule)
                .toList();
    }

    private List<Reason> baseScoreReasons(Evaluation evaluation) {
        long qualityScore = Math.round(evaluation.qualityResult().qualityScore());
        Reason qualityReason = Reason.ofBaseScore("Qualidade do produto: " + qualityScore + " de 100.");

        if (evaluation.unevaluatedIngredientCount() == 0) {
            return List.of(qualityReason);
        }

        Reason unevaluatedReason = Reason.ofBaseScore(evaluation.unevaluatedIngredientCount() + " de "
                + evaluation.ingredientCount() + " ingredientes ainda sem avaliação de saúde e ambiental.");
        return List.of(qualityReason, unevaluatedReason);
    }

    private String allergyText(AllergyMatch match) {
        return "Contém " + String.join(", ", match.ingredientNames()) + ", ligado à sua alergia a "
                + match.allergyName() + " (gravidade " + severityLabel(match.severity()) + ").";
    }

    private String summaryOf(Verdict verdict, List<Reason> reasons) {
        String bandLabel = bandLabel(verdict.recommendationLevel());
        String headline = "Nota " + verdict.finalScore() + " de 100 - " + bandLabel + ".";

        String mainReasons = reasons.stream()
                .filter(reason -> reason.source() != ReasonSource.BASE_SCORE)
                .limit(MAX_REASONS_IN_SUMMARY)
                .map(this::summaryTextOf)
                .collect(Collectors.joining(" "));

        if (mainReasons.isEmpty()) {
            return headline;
        }
        return headline + " " + mainReasons;
    }

    private String summaryTextOf(Reason reason) {
        if (reason.warning()) {
            return WARNING_PREFIX + reason.text();
        }
        return reason.text();
    }

    private String bandLabel(RecommendationLevel level) {
        return switch (level) {
            case IDEAL -> "Ideal";
            case RECOMMENDED -> "Recomendado";
            case ACCEPTABLE -> "Aceitável";
            case NOT_RECOMMENDED -> "Não recomendado";
            case CONTRAINDICATED -> "Contraindicado";
        };
    }

    private String severityLabel(RiskLevel severity) {
        return switch (severity) {
            case LOW -> "leve";
            case MEDIUM -> "média";
            case HIGH -> "alta";
            case CRITICAL -> "grave";
        };
    }
}
