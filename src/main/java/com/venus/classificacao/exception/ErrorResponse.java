package com.venus.classificacao.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        ClassificationErrorCode code,
        String message,
        String path,
        List<String> details
) {
}
