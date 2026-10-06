package com.venus.classificacao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ClassificationBreakdownResponse(
        @Schema(description = "Nota de qualidade do produto, de 0 a 100, sem olhar o perfil. Nula no GET.",
                example = "74", nullable = true)
        Integer qualityScore,
        @Schema(description = "Pontos de qualidade: a nota de qualidade vale até 35 pontos na nota final. Nulo no GET.",
                example = "25.9", nullable = true)
        BigDecimal qualityPoints,
        @Schema(description = "Pontos de perfil: as perguntas do perfil valem até 65 pontos na nota final. Nulo quando "
                + "nenhuma pergunta entrou na conta, e no GET.", example = "15.3", nullable = true)
        BigDecimal profilePoints,
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
        Integer performanceScore
) {
}
