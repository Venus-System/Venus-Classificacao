package com.venus.classificacao.dto.response;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ClassificationResponse(
        @Schema(description = "Usuário analisado.", example = "42")
        Long userId,
        @Schema(description = "Versão do produto analisada.", example = "118")
        Long productVersionId,
        @Schema(description = "Modelo de score usado no cálculo.", example = "1")
        Long scoringModelId,
        @Schema(description = "Nota final de 0 a 100, depois das travas de bloqueio e de alergia.", example = "41")
        Integer finalScore,
        @Schema(description = "Faixa de recomendação da nota final.")
        RecommendationLevel recommendationLevel,
        @Schema(description = "Nível de risco do produto para o usuário.")
        RiskLevel riskLevel,
        @Schema(description = "Quanto o produto combina com o perfil, de 0 a 100. Nulo quando nenhuma pergunta do perfil "
                + "entrou na conta.", example = "23.53", nullable = true)
        BigDecimal compatibilityPercentage,
        @Schema(description = "Quantos ingredientes a versão tem.", example = "18")
        Integer ingredientCount,
        @Schema(description = "Quantos ingredientes ainda não têm avaliação de saúde e ambiental.", example = "0")
        Integer unevaluatedIngredientCount,
        @Schema(description = "Notas que compõem a nota final.")
        ClassificationBreakdownResponse breakdown,
        @Schema(description = "Motivos da nota. Bloqueio e alergia vêm sempre primeiro.")
        List<ClassificationReasonResponse> reasons,
        @Schema(description = "Resumo em texto da nota e dos dois principais motivos.",
                example = "Nota 41 de 100 - Não recomendado.")
        String summary,
        @Schema(description = "Quando a análise foi calculada.")
        OffsetDateTime calculatedAt,
        @Schema(description = "Tempo do cálculo em milissegundos.", example = "35")
        Integer processingTimeMs
) {
}
