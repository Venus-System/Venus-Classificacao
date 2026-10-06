package com.venus.classificacao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BaseScoreSummaryResponse(
        @Schema(description = "Modelo de score usado no cálculo.", example = "1")
        Long scoringModelId,
        @Schema(description = "Quantas versões atuais de produto existem.", example = "1200")
        Integer totalVersions,
        @Schema(description = "Quantas tiveram a nota base calculada e gravada.", example = "1185")
        Integer recalculatedCount,
        @Schema(description = "Versões puladas, com o motivo. Para conferir as notas gravadas, use o "
                + "GET /api/product-scores do Venus-CRUD.")
        List<SkippedVersionResponse> skipped,
        @Schema(description = "Tempo total do cálculo em milissegundos.", example = "95000")
        Integer processingTimeMs
) {
}
