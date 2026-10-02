package com.venus.classificacao.service.question;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public record ProfileResult(List<QuestionScore> questionScores) {

    public ProfileResult {
        questionScores = List.copyOf(questionScores);
    }

    public OptionalDouble compatibilityRatio() {
        List<QuestionScore> applicableQuestions = questionScores.stream()
                .filter(QuestionScore::applicable)
                .toList();

        double possiblePoints = applicableQuestions.stream().mapToDouble(QuestionScore::maxPoints).sum();
        if (possiblePoints == 0) {
            return OptionalDouble.empty();
        }

        double earnedPoints = applicableQuestions.stream().mapToDouble(QuestionScore::earnedPoints).sum();
        return OptionalDouble.of(earnedPoints / possiblePoints);
    }

    public List<MatchedRule> distinctMatchedRules() {
        Map<Long, MatchedRule> matchedRuleById = new LinkedHashMap<>();

        questionScores.stream()
                .flatMap(questionScore -> questionScore.matchedRules().stream())
                .forEach(rule -> matchedRuleById.putIfAbsent(rule.ruleId(), rule));

        return List.copyOf(matchedRuleById.values());
    }

    public boolean hasBlock() {
        return distinctMatchedRules().stream().anyMatch(MatchedRule::isBlock);
    }

    public List<String> preferenceNotes() {
        return questionScores.stream()
                .flatMap(questionScore -> questionScore.preferenceNotes().stream())
                .toList();
    }
}
