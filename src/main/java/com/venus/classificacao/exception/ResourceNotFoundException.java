package com.venus.classificacao.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final ClassificationErrorCode code;

    public ResourceNotFoundException(ClassificationErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ClassificationErrorCode getCode() {
        return code;
    }
}
