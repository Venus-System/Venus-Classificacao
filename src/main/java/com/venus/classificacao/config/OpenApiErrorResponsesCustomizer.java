package com.venus.classificacao.config;

import com.venus.classificacao.exception.ClassificationErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

@Component
public class OpenApiErrorResponsesCustomizer implements OperationCustomizer {

    static final String ERROR_SCHEMA_NAME = "ErrorResponse";

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/" + ERROR_SCHEMA_NAME;
    private static final String APPLICATION_JSON = "application/json";
    private static final String OK = "200";
    private static final String EXEMPLO_TIMESTAMP = "2026-09-22T14:30:00-03:00";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        boolean hasRequestBody = false;
        boolean hasPathVariable = false;
        boolean hasTypedParameter = false;

        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(RequestBody.class)) {
                hasRequestBody = true;
            }
            if (parameter.hasParameterAnnotation(PathVariable.class)) {
                hasPathVariable = true;
            }
            if (isTypedParameter(parameter)) {
                hasTypedParameter = true;
            }
        }

        ApiResponses responses = operation.getResponses();
        applySuccessStatus(responses, handlerMethod);

        if (hasRequestBody || hasTypedParameter) {
            addResponse(responses, HttpStatus.BAD_REQUEST,
                    "Dados inválidos. O campo details traz uma linha por campo ou parâmetro recusado.",
                    example(HttpStatus.BAD_REQUEST, null, "Validation failed", "/api/classifications",
                            List.of("productVersionId: nao deve ser nulo")));
        }
        if (hasRequestBody) {
            addResponse(responses, HttpStatus.NOT_FOUND,
                    "Não achou o perfil do usuário, a versão do produto ou o modelo de score. O campo code diz qual.",
                    example(HttpStatus.NOT_FOUND, ClassificationErrorCode.PROFILE_NOT_FOUND,
                            "Perfil nao encontrado para o usuario com id 42", "/api/classifications", List.of()));
            addResponse(responses, HttpStatus.UNPROCESSABLE_ENTITY,
                    "O produto não pode ser classificado: a versão está em revisão (VERSION_UNDER_REVIEW) ou não tem "
                            + "ingrediente (NO_INGREDIENTS). O campo code diz qual.",
                    example(HttpStatus.UNPROCESSABLE_ENTITY, ClassificationErrorCode.NO_INGREDIENTS,
                            "O produto nao tem nenhum ingrediente cadastrado", "/api/classifications", List.of()));
        } else if (hasPathVariable) {
            addResponse(responses, HttpStatus.NOT_FOUND,
                    "Não existe análise salva para esse filtro, ou não achou o produto, a versão ou o modelo. "
                            + "O campo code diz qual.",
                    example(HttpStatus.NOT_FOUND, ClassificationErrorCode.ANALYSIS_NOT_FOUND,
                            "Nenhuma analise encontrada para o usuario 42 na versao 118 com o modelo 1",
                            "/api/classifications/user/42/product-version/118", List.of()));
        }
        addResponse(responses, HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro inesperado no servidor.",
                example(HttpStatus.INTERNAL_SERVER_ERROR, null, "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                        "/api/classifications", List.of()));
        addResponse(responses, HttpStatus.SERVICE_UNAVAILABLE,
                "Banco de dados indisponível no momento. Tentar de novo depois costuma resolver.",
                example(HttpStatus.SERVICE_UNAVAILABLE, null,
                        "O banco de dados esta indisponivel no momento. Tente novamente mais tarde.",
                        "/api/classifications", List.of()));

        return operation;
    }

    private void applySuccessStatus(ApiResponses responses, HandlerMethod handlerMethod) {
        ApiResponse sucesso = responses.remove(OK);
        if (sucesso == null) {
            return;
        }
        if (handlerMethod.hasMethodAnnotation(PostMapping.class) && !handlerMethod.hasMethodAnnotation(ResponseStatus.class)) {
            responses.addApiResponse(String.valueOf(HttpStatus.CREATED.value()), sucesso
                    .description("Análise calculada e gravada.")
                    .addHeaderObject("Location", new Header()
                            .description("URI do GET que devolve esta análise.")
                            .schema(new StringSchema().format("uri"))));
            return;
        }
        responses.addApiResponse(OK, sucesso);
    }

    private boolean isTypedParameter(MethodParameter parameter) {
        boolean bound = parameter.hasParameterAnnotation(PathVariable.class)
                || parameter.hasParameterAnnotation(RequestParam.class);
        return bound && !String.class.equals(parameter.getParameterType());
    }

    static Map<String, Object> example(HttpStatus status, ClassificationErrorCode code, String message, String path,
            List<String> details) {
        Map<String, Object> exemplo = new LinkedHashMap<>();
        exemplo.put("timestamp", EXEMPLO_TIMESTAMP);
        exemplo.put("status", status.value());
        exemplo.put("error", status.getReasonPhrase());
        exemplo.put("code", code == null ? null : code.name());
        exemplo.put("message", message);
        exemplo.put("path", path);
        exemplo.put("details", details);
        return exemplo;
    }

    static void addResponse(ApiResponses responses, HttpStatus status, String description, Map<String, Object> example) {
        String code = String.valueOf(status.value());
        if (responses.containsKey(code)) {
            return;
        }
        responses.addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(APPLICATION_JSON, new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .example(example))));
    }
}
