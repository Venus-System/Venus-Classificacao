package com.venus.classificacao.controller;

import com.venus.classificacao.dto.request.ClassificationRequest;
import com.venus.classificacao.dto.response.ClassificationResponse;
import com.venus.classificacao.service.ClassificationService;
import com.venus.classificacao.service.SavedClassificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/classifications")
@Tag(name = "Classificações", description = "Nota do produto para o perfil do usuário, com os motivos da nota.")
public class ClassificationController {

    private static final String SCORING_MODEL_DESCRIPTION =
            "Modelo de score da análise. Sem ele, usa o modelo ativo de maior id.";

    private final ClassificationService classificationService;
    private final SavedClassificationService savedClassificationService;

    public ClassificationController(ClassificationService classificationService,
            SavedClassificationService savedClassificationService) {
        this.classificationService = classificationService;
        this.savedClassificationService = savedClassificationService;
    }

    @Operation(operationId = "classificationCreate",
            summary = "Calcula, grava e devolve a nota do produto para o perfil do usuário")
    @PreAuthorize("@ownership.canAccessUser(#request.userId())")
    @PostMapping
    public ResponseEntity<ClassificationResponse> create(@Valid @RequestBody ClassificationRequest request) {
        ClassificationResponse created = classificationService.classify(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/user/{userId}/product-version/{versionId}")
                .queryParam("scoringModelId", created.scoringModelId())
                .buildAndExpand(created.userId(), created.productVersionId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(operationId = "classificationFindByVersion",
            summary = "Devolve a última análise salva do usuário para a versão do produto")
    @PreAuthorize("@ownership.canAccessUser(#userId)")
    @GetMapping("/user/{userId}/product-version/{versionId}")
    public ResponseEntity<ClassificationResponse> findByVersion(
            @PathVariable Long userId,
            @PathVariable Long versionId,
            @Parameter(description = SCORING_MODEL_DESCRIPTION) @RequestParam(required = false) Long scoringModelId) {
        return ResponseEntity.ok(savedClassificationService.findLatestByVersion(userId, versionId, scoringModelId));
    }
}
