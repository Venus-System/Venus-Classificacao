package com.venus.classificacao.dto.response;

import com.venus.classificacao.exception.ClassificationErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record SkippedVersionResponse(
        @Schema(description = "Produto da versão pulada.", example = "7")
        Long productId,
        @Schema(description = "Versão pulada.", example = "118")
        Long productVersionId,
        @Schema(description = "Por que foi pulada: versão em revisão (VERSION_UNDER_REVIEW) ou sem ingrediente "
                + "(NO_INGREDIENTS).", example = "NO_INGREDIENTS")
        ClassificationErrorCode code
) {
}
