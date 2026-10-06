package com.venus.classificacao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record BaseScoreResponse(
        @Schema(description = "Versão do produto calculada.", example = "118")
        Long productVersionId,
        @Schema(description = "Modelo de score usado no cálculo.", example = "1")
        Long scoringModelId,
        @Schema(description = "Nota base do produto, de 0 a 100, sem olhar o perfil. É a média ponderada das notas "
                + "abaixo, sem contar as nulas.", example = "74")
        Integer qualityScore,
        @Schema(description = "Nota de saúde, de 0 a 100. Nula quando nenhum ingrediente tem avaliação.", example = "89",
                nullable = true)
        Integer healthScore,
        @Schema(description = "Nota ambiental, de 0 a 100. Nula sem ingrediente avaliado e sem embalagem avaliada.",
                example = "63", nullable = true)
        Integer environmentalScore,
        @Schema(description = "Nota ética, de 0 a 100.", example = "95")
        Integer ethicalScore,
        @Schema(description = "Nota de desempenho, de 0 a 100. Nula quando nenhum ingrediente tem avaliação.",
                example = "39", nullable = true)
        Integer performanceScore,
        @Schema(description = "Quantos ingredientes a versão tem.", example = "18")
        Integer ingredientCount,
        @Schema(description = "Quantos ingredientes ainda não têm avaliação. Explica as notas nulas.", example = "0")
        Integer unevaluatedIngredientCount,
        @Schema(description = "Quando a nota foi calculada.")
        OffsetDateTime calculatedAt
) {
}
