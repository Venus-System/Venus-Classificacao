package com.venus.classificacao.exception;

public class DataAccessFailureException extends RuntimeException {

    public DataAccessFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
