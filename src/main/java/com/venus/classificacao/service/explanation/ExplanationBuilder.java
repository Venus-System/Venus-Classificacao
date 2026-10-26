package com.venus.classificacao.service.explanation;

import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import com.venus.classificacao.service.question.MatchedRule;
import com.venus.classificacao.service.verdict.AllergyMatch;
import com.venus.classificacao.service.verdict.Evaluation;
import com.venus.classificacao.service.verdict.FinalScorePolicy;
import com.venus.classificacao.service.verdict.Verdict;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class ExplanationBuilder {

    private static final int MAX_REASONS_IN_SUMMARY = 2;
    private static final String WARNING_PREFIX = "Atenção: ";

    public Explanation build(Evaluation evaluation) {
        List<Reason> reasons = reasonsOf(evaluation);
        String summary = summaryOf(evaluation);

        return new Explanation(reasons, summary);
    }

    public List<Reason> ruleReasonsOf(List<MatchedRule> matchedRules) {
        return Stream.of(blockReasons(matchedRules), ruleReasons(matchedRules))
                .flatMap(List::stream)
                .toList();
    }

    private List<MatchedRule> blockRules(List<MatchedRule> matchedRules) {
        return matchedRules.stream()
                .filter(MatchedRule::isBlock)
                .toList();
    }

    private List<MatchedRule> scoringRules(List<MatchedRule> matchedRules) {
        return matchedRules.stream()
                .filter(rule -> !rule.isBlock())
                .sorted(Comparator.comparing((MatchedRule rule) -> rule.impact().abs()).reversed())
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
        return blockRules(matchedRules).stream()
                .map(Reason::ofRule)
                .toList();
    }

    private List<Reason> allergyReasons(List<AllergyMatch> allergies) {
        return allergies.stream()
                .map(match -> Reason.ofAllergy(allergyText(match), match.ingredientNames()))
                .toList();
    }

    private List<Reason> preferenceReasons(List<String> preferenceNotes) {
        return preferenceNotes.stream()
                .map(Reason::ofPreference)
                .toList();
    }

    private List<Reason> ruleReasons(List<MatchedRule> matchedRules) {
        return scoringRules(matchedRules).stream()
                .map(Reason::ofRule)
                .toList();
    }

    private List<Reason> baseScoreReasons(Evaluation evaluation) {
        long qualityScore = Math.round(evaluation.qualityResult().qualityScore());
        Reason qualityReason = Reason.ofBaseScore(
                "A qualidade do produto, sem olhar o seu perfil, é " + qualityScore + " de 100.");

        int unevaluatedCount = evaluation.unevaluatedIngredientCount();
        if (unevaluatedCount == 0) {
            return List.of(qualityReason);
        }

        String unevaluatedText = unevaluatedCount == 1
                ? " ingredientes ainda não foi avaliado, então ficou fora da nota de qualidade."
                : " ingredientes ainda não foram avaliados, então ficaram fora da nota de qualidade.";
        Reason unevaluatedReason = Reason.ofBaseScore(
                unevaluatedCount + " dos " + evaluation.ingredientCount() + unevaluatedText);
        return List.of(qualityReason, unevaluatedReason);
    }

    private String allergyText(AllergyMatch match) {
        List<String> names = match.ingredientNames();
        String belongs = names.size() == 1 ? ", que está" : ", que estão";
        return "Tem " + joinNames(names) + belongs + " na sua alergia a " + match.allergyName() + ". "
                + allergyOutcome(match.severity());
    }

    private String joinNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        String allButLast = String.join(", ", names.subList(0, names.size() - 1));
        return allButLast + " e " + names.get(names.size() - 1);
    }

    private String allergyOutcome(RiskLevel severity) {
        return switch (severity) {
            case CRITICAL, HIGH -> "Por isso esse produto não é indicado para você.";
            case MEDIUM -> "Por isso a nota fica no máximo em " + FinalScorePolicy.MEDIUM_ALLERGY_MAX_SCORE + ".";
            case LOW -> "Por isso a nota caiu " + FinalScorePolicy.LOW_ALLERGY_DISCOUNT + " pontos.";
        };
    }

    private String summaryOf(Evaluation evaluation) {
        Verdict verdict = evaluation.verdict();
        String bandLabel = bandLabel(verdict.recommendationLevel());
        String headline = "Nota " + verdict.finalScore() + " de 100 - " + bandLabel + ".";

        List<SummaryLine> lines = summaryLinesOf(evaluation);
        if (lines.isEmpty()) {
            return headline;
        }
        return headline + " " + joinWithOneWarning(lines);
    }

    private List<SummaryLine> summaryLinesOf(Evaluation evaluation) {
        List<MatchedRule> matchedRules = evaluation.profileResult().distinctMatchedRules();
        Map<String, SummaryLine> lineByText = new LinkedHashMap<>();

        Stream.of(
                        ruleLines(blockRules(matchedRules)),
                        allergyLines(evaluation.allergies()),
                        preferenceLines(evaluation.profileResult().preferenceNotes()),
                        ruleLines(scoringRules(matchedRules)))
                .flatMap(List::stream)
                .forEach(line -> lineByText.putIfAbsent(line.text(), line));

        return lineByText.values().stream()
                .limit(MAX_REASONS_IN_SUMMARY)
                .toList();
    }

    private List<SummaryLine> ruleLines(List<MatchedRule> rules) {
        Map<RuleGroup, List<MatchedRule>> rulesByGroup = rules.stream()
                .collect(Collectors.groupingBy(RuleGroup::of, LinkedHashMap::new, Collectors.toList()));

        return rulesByGroup.entrySet().stream()
                .map(group -> groupLine(group.getKey(), group.getValue()))
                .toList();
    }

    private SummaryLine groupLine(RuleGroup group, List<MatchedRule> rules) {
        boolean warning = rules.get(0).isWarning();
        if (rules.get(0).hasOwnReason()) {
            return new SummaryLine(group.text(), warning);
        }

        long ingredientCount = rules.stream()
                .map(MatchedRule::ingredientId)
                .distinct()
                .count();
        String ingredientWord = ingredientCount == 1 ? " ingrediente com " : " ingredientes com ";
        return new SummaryLine(ingredientCount + ingredientWord + group.text() + ".", warning);
    }

    private List<SummaryLine> allergyLines(List<AllergyMatch> allergies) {
        return allergies.stream()
                .map(match -> new SummaryLine(allergyText(match), true))
                .toList();
    }

    private List<SummaryLine> preferenceLines(List<String> preferenceNotes) {
        return preferenceNotes.stream()
                .map(note -> new SummaryLine(note, false))
                .toList();
    }

    private String joinWithOneWarning(List<SummaryLine> lines) {
        int firstWarning = IntStream.range(0, lines.size())
                .filter(index -> lines.get(index).warning())
                .findFirst()
                .orElse(-1);

        return IntStream.range(0, lines.size())
                .mapToObj(index -> index == firstWarning
                        ? WARNING_PREFIX + lines.get(index).text()
                        : lines.get(index).text())
                .collect(Collectors.joining(" "));
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

    private record SummaryLine(String text, boolean warning) {
    }

    private record RuleGroup(EffectType effectType, String text) {

        static RuleGroup of(MatchedRule rule) {
            if (rule.hasOwnReason()) {
                return new RuleGroup(rule.effectType(), rule.explanationText());
            }
            return new RuleGroup(rule.effectType(), rule.effectText() + " (" + rule.profileLabel() + ")");
        }
    }
}
