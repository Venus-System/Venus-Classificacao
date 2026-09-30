package com.venus.classificacao.config;

import com.venus.classificacao.exception.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI venusClassificacaoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Venus Classificação API")
                        .description(description())
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Venus System")
                                .url("https://github.com/Venus-System/Venus-Classificacao")))
                .components(new Components()
                        .addSchemas(OpenApiErrorResponsesCustomizer.ERROR_SCHEMA_NAME, errorResponseSchema())
                        .addSecuritySchemes(OpenApiSecurityCustomizer.SECURITY_SCHEME_NAME, bearerScheme()));
    }

    private String description() {
        return """
                API de classificação do Venus System: calcula o score de um produto para o perfil do usuário, \
                grava a análise e explica o porquê da nota.

                **Erros** — toda resposta de erro usa o mesmo corpo (`ErrorResponse`), com `timestamp`, \
                `status`, `error`, `code`, `message`, `path` e `details`. O `code` só vem preenchido nos 404 e 422 \
                da classificação e diz o motivo sem precisar ler a mensagem. O `details` só vem preenchido em erro \
                de validação, com uma linha por campo recusado.

                **Autenticação** — todas as rotas exigem o cabeçalho `Authorization: Bearer <token>`, com o \
                **ID token do Firebase** que o app recebe no login do usuário. Sem token, ou com token inválido ou \
                vencido, a resposta é 401. Com um token válido, mas de outro usuário ou de uma conta que não está \
                ativa, 403. Use o botão **Authorize** para testar aqui.
                """;
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("ID token do Firebase (usuário do app).");
    }

    private Schema<?> errorResponseSchema() {
        return ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class).resolveAsRef(false))
                .schema;
    }
}
