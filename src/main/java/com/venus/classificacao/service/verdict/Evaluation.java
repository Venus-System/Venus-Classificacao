package com.venus.classificacao.service.verdict;

import com.venus.classificacao.service.quality.QualityResult;
import com.venus.classificacao.service.question.ProfileResult;
import java.util.List;

public record Evaluation(
        QualityResult qualityResult,
        ProfileResult profileResult,
        CombinedScore combinedScore,
        List<AllergyMatch> allergies,
        Verdict verdict,
        int ingredientCount,
        int unevaluatedIngredientCount
) {

    public Evaluation {
        allergies = List.copyOf(allergies);
    }
}
