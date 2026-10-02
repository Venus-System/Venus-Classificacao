package com.venus.classificacao.dto.response;

import com.venus.classificacao.service.explanation.ReasonSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ClassificationReasonResponse(
        @Schema(description = "De onde vem o motivo: regra de ingrediente, alergia, preferência ou qualidade do produto.")
        ReasonSource source,
        @Schema(description = "O motivo em texto, para mostrar no app.",
                example = "Niacinamida tem uma regra de bônus para pele-acneica.")
        String text,
        @Schema(description = "Se o motivo é um alerta (bloqueio, penalidade ou alergia).", example = "false")
        Boolean warning,
        @Schema(description = "Pontos da regra no banco. Nulo em motivo que não vem de regra.", example = "5",
                nullable = true)
        Integer scoreDelta,
        @Schema(description = "Peso da regra. Nulo em motivo que não vem de regra.", example = "1.00", nullable = true)
        BigDecimal weight,
        @Schema(description = "Efeito da regra na pergunta: pontos vezes peso, com sinal. Nulo em motivo que não vem de "
                + "regra.", example = "5.00", nullable = true)
        BigDecimal impact,
        @Schema(description = "Ingrediente da regra. Nulo em motivo que não vem de regra.", example = "Niacinamida",
                nullable = true)
        String ingredientName,
        @Schema(description = "Etiqueta do perfil que a regra usa. Nula em motivo que não vem de regra.",
                example = "pele-acneica", nullable = true)
        String profileTagSlug
) {
}
