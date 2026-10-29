package com.venus.classificacao.service.basescore;

import com.venus.classificacao.service.quality.QualityScores;
import java.time.OffsetDateTime;

public record BaseScoreResult(
        Long productVersionId,
        Long scoringModelId,
        QualityScores scores,
        int ingredientCount,
        int unevaluatedIngredientCount,
        OffsetDateTime calculatedAt
) {
}
