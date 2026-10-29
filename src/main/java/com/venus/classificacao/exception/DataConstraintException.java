package com.venus.classificacao.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class DataConstraintException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> details;

    public DataConstraintException(HttpStatus status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }
}
