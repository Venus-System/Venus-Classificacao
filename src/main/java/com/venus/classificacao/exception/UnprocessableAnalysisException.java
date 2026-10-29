package com.venus.classificacao.exception;

public class UnprocessableAnalysisException extends RuntimeException {

    private final ClassificationErrorCode code;

    public UnprocessableAnalysisException(ClassificationErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ClassificationErrorCode getCode() {
        return code;
    }
}
