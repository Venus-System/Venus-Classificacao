package com.venus.classificacao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ClassificationRequest(
        @Schema(description = "Usuário que está analisando o produto. Tem que ser o dono do token.", example = "42")
        @NotNull
        Long userId,
        @Schema(description = "Versão exata da fórmula do produto.", example = "118")
        @NotNull
        Long productVersionId,
        @Schema(description = "Modelo de score. Sem ele, usa o modelo ativo de maior id.", example = "1", nullable = true)
        Long scoringModelId
) {
}
