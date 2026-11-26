package com.observatory.exception;

/**
 * Thrown for all expression-related errors (invalid syntax, undefined variables,
 * division by zero, token-limit exceeded, etc.).
 * Carries a machine-readable code and optional source position.
 */
public class EvaluationException extends RuntimeException {

    private final String code;
    private final Integer position;

    public EvaluationException(String code, String message, Integer position) {
        super(message);
        this.code = code;
        this.position = position;
    }

    public String getCode() { return code; }

    /** 0-based character index in the original expression, or null. */
    public Integer getPosition() { return position; }
}
