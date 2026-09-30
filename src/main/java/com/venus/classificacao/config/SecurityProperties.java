package com.venus.classificacao.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "venus.security")
public record SecurityProperties(
        @Valid @NotNull Firebase firebase
) {

    public record Firebase(@NotBlank String projectId) {
    }
}
