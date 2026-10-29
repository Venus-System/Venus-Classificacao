package com.venus.classificacao.controller;

import com.venus.classificacao.dto.response.BaseScoreResponse;
import com.venus.classificacao.dto.response.BaseScoreSummaryResponse;
import com.venus.classificacao.service.BaseScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base-scores")
@Tag(name = "Notas Base", description = "Nota base do produto, sem olhar o perfil, calculada e gravada pelo "
        + "administrador. Para ler as notas gravadas, use o GET /api/product-scores do Venus-CRUD.")
public class BaseScoreController {

    private static final String SCORING_MODEL_DESCRIPTION =
            "Modelo de score do cálculo. Sem ele, usa o modelo ativo de maior id.";
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    private final BaseScoreService baseScoreService;

    public BaseScoreController(BaseScoreService baseScoreService) {
        this.baseScoreService = baseScoreService;
    }

    @Operation(operationId = "baseScoreRecalculateVersion",
            summary = "Calcula e grava a nota base de uma versão do produto",
            description = "Chamar no fim do cadastro do produto, depois dos ingredientes e da embalagem. Também serve "
                    + "para uma versão que ainda não é a atual.")
    @ApiResponse(responseCode = "404",
            description = "Não achou a versão do produto ou o modelo de score. O campo code diz qual.",
            content = @Content(mediaType = "application/json", schema = @Schema(ref = ERROR_SCHEMA_REF)))
    @ApiResponse(responseCode = "422",
            description = "A versão não pode ser calculada: está em revisão (VERSION_UNDER_REVIEW) ou não tem "
                    + "ingrediente (NO_INGREDIENTS). O campo code diz qual.",
            content = @Content(mediaType = "application/json", schema = @Schema(ref = ERROR_SCHEMA_REF)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/product-version/{versionId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<BaseScoreResponse> recalculateVersion(
            @PathVariable Long versionId,
            @Parameter(description = SCORING_MODEL_DESCRIPTION) @RequestParam(required = false) Long scoringModelId) {
        return ResponseEntity.ok(baseScoreService.recalculateVersion(versionId, scoringModelId));
    }

    @Operation(operationId = "baseScoreRecalculateAll",
            summary = "Calcula e grava a nota base da versão atual de todos os produtos",
            description = "Versão em revisão ou sem ingrediente é pulada e aparece em skipped. Com o catálogo grande o "
                    + "request demora; se cair no meio, o que já foi gravado fica e é só rodar de novo.")
    @ApiResponse(responseCode = "404",
            description = "Não achou o modelo de score pedido, ou não existe modelo ativo. O campo code diz qual.",
            content = @Content(mediaType = "application/json", schema = @Schema(ref = ERROR_SCHEMA_REF)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<BaseScoreSummaryResponse> recalculateAll(
            @Parameter(description = SCORING_MODEL_DESCRIPTION) @RequestParam(required = false) Long scoringModelId) {
        return ResponseEntity.ok(baseScoreService.recalculateAll(scoringModelId));
    }
}
