package com.venus.classificacao.exception;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

public final class DataIntegrityViolationTranslator {

    private static final Logger log = LoggerFactory.getLogger(DataIntegrityViolationTranslator.class);

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String NOT_NULL_VIOLATION = "23502";
    private static final String CHECK_VIOLATION = "23514";

    private static final Pattern COLUMN_PATTERN = Pattern.compile("column \"([^\"]+)\"");

    private DataIntegrityViolationTranslator() {
    }

    public static DataConstraintException translate(DataIntegrityViolationException ex) {
        String sqlState = findSqlState(ex);
        String constraint = findConstraintName(ex);
        String column = NOT_NULL_VIOLATION.equals(sqlState) ? findColumnName(ex) : null;

        log.warn("Violacao de integridade de dados: sqlState={}, constraint={}, column={}", sqlState, constraint, column);

        List<String> details = new ArrayList<>();
        if (constraint != null) {
            details.add("constraint: " + constraint);
        }
        if (column != null) {
            details.add("column: " + column);
        }

        if (ex instanceof DuplicateKeyException || UNIQUE_VIOLATION.equals(sqlState)) {
            return new DataConstraintException(HttpStatus.CONFLICT,
                    "Ja existe um registro com os dados informados.", details);
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState)) {
            return new DataConstraintException(HttpStatus.CONFLICT,
                    "O registro referenciado nao existe ou ainda esta vinculado a outros registros.", details);
        }
        if (NOT_NULL_VIOLATION.equals(sqlState)) {
            return new DataConstraintException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Um campo obrigatorio nao foi informado.", details);
        }
        if (CHECK_VIOLATION.equals(sqlState)) {
            return new DataConstraintException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Os dados informados violam uma regra de consistencia do banco de dados.", details);
        }
        if (sqlState != null) {
            details.add("sqlState: " + sqlState);
        }
        return new DataConstraintException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Os dados informados violam uma regra de integridade do banco de dados.", details);
    }

    private static String findSqlState(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    private static String findConstraintName(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }

    private static String findColumnName(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException && cause.getMessage() != null) {
                Matcher matcher = COLUMN_PATTERN.matcher(cause.getMessage());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }
}
