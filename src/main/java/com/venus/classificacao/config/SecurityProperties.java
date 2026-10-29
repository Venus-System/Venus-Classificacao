package com.venus.classificacao.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "venus.security")
public record SecurityProperties(
        @Valid @NotNull AdminToken adminToken,
        @Valid @NotNull Firebase firebase
) {

    public record AdminToken(@NotBlank String secret) {

        private static final int MIN_SECRET_BYTES = 32;

        public AdminToken {
            if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalArgumentException("ADMIN_JWT_SECRET precisa ter pelo menos 32 bytes.");
            }
        }
    }

    public record Firebase(@NotBlank String projectId) {
    }
}
