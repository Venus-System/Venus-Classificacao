package com.venus.classificacao.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;

public final class DataAccessFailureTranslator {

    private DataAccessFailureTranslator() {
    }

    public static RuntimeException translate(DataAccessException ex, String errorMessage) {
        if (isTemporary(ex)) {
            return new ServiceUnavailableException(errorMessage + ". Tente novamente mais tarde.", ex);
        }
        return new DataAccessFailureException(errorMessage, ex);
    }

    private static boolean isTemporary(DataAccessException ex) {
        return ex instanceof TransientDataAccessException
                || ex instanceof RecoverableDataAccessException
                || ex instanceof DataAccessResourceFailureException;
    }
}
