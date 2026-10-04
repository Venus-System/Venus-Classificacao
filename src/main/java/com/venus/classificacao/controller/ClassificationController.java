package com.venus.classificacao.controller;

import com.venus.classificacao.dto.request.ClassificationRequest;
import com.venus.classificacao.dto.response.ClassificationResponse;
import com.venus.classificacao.service.ClassificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/classifications")
@Tag(name = "Classificações", description = "Nota do produto para o perfil do usuário, com os motivos da nota.")
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
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
}
