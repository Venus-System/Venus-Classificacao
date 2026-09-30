package com.venus.classificacao.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.venus.classificacao.security.SecurityErrorHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String UNEXPECTED_ERROR_MESSAGE = "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(UnprocessableAnalysisException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessableAnalysis(UnprocessableAnalysisException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataConstraintException.class)
    public ResponseEntity<ErrorResponse> handleDataConstraint(DataConstraintException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, ex.getDetails());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return handleDataConstraint(DataIntegrityViolationTranslator.translate(ex), request);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleCannotCreateTransaction(CannotCreateTransactionException ex, HttpServletRequest request) {
        log.error("Falha ao abrir transacao com o banco de dados em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "O banco de dados esta indisponivel no momento. Tente novamente mais tarde.", request, List.of());
    }

    @ExceptionHandler(DataAccessFailureException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessFailure(DataAccessFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE, request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, SecurityErrorHandler.ACCESS_DENIED_MESSAGE, request, List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parametro invalido", request, List.of(describeMismatch(ex)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido", request, describeUnreadableBody(ex));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parametro obrigatorio ausente", request,
                List.of(ex.getParameterName() + ": parametro obrigatorio do tipo " + ex.getParameterType()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Endpoint nao encontrado", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        List<HttpMethod> supported = ex.getSupportedHttpMethods() != null ? List.copyOf(ex.getSupportedHttpMethods()) : List.of();
        List<String> details = supported.isEmpty() ? List.of() : List.of("Metodos aceitos: " + supported);
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP nao suportado", request, details);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        List<String> details = ex.getSupportedMediaTypes().isEmpty()
                ? List.of()
                : List.of("Tipos aceitos: " + ex.getSupportedMediaTypes());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type nao suportado", request, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE, request, List.of());
    }

    private String describeMismatch(MethodArgumentTypeMismatchException ex) {
        return describeInvalidValue(ex.getName(), ex.getValue(), ex.getRequiredType());
    }

    private List<String> describeUnreadableBody(HttpMessageNotReadableException ex) {
        if (!(ex.getCause() instanceof MismatchedInputException mismatch) || mismatch.getPath().isEmpty()) {
            return List.of();
        }
        String field = describePath(mismatch.getPath());
        if (mismatch instanceof InvalidFormatException invalidFormat) {
            return List.of(describeInvalidValue(field, invalidFormat.getValue(), invalidFormat.getTargetType()));
        }
        Class<?> targetType = mismatch.getTargetType();
        String expected = targetType != null ? targetType.getSimpleName() : "desconhecido";
        return List.of(field + ": valor com formato invalido, esperado " + expected);
    }

    private String describePath(List<JsonMappingException.Reference> path) {
        StringBuilder field = new StringBuilder();
        for (JsonMappingException.Reference reference : path) {
            if (reference.getFieldName() == null) {
                field.append('[').append(reference.getIndex()).append(']');
            } else {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(reference.getFieldName());
            }
        }
        return field.toString();
    }

    private String describeInvalidValue(String name, Object value, Class<?> requiredType) {
        if (requiredType != null && requiredType.isEnum()) {
            return name + ": '" + value + "' nao e um valor aceito. Valores aceitos: "
                    + Arrays.toString(requiredType.getEnumConstants());
        }
        String expected = requiredType != null ? requiredType.getSimpleName() : "desconhecido";
        return name + ": '" + value + "' nao e um valor valido do tipo " + expected;
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request, List<String> details) {
        return build(status, null, message, request, details);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ClassificationErrorCode code, String message,
            HttpServletRequest request, List<String> details) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
