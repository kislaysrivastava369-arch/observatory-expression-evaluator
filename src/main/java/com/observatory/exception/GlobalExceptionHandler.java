package com.observatory.exception;

import com.observatory.model.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into structured JSON error responses.
 * Never exposes stack traces to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** All expression-related errors → 400 Bad Request */
    @ExceptionHandler(EvaluationException.class)
    public ResponseEntity<ErrorResponse> handleEvaluationException(EvaluationException ex) {
        ErrorResponse body = new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getPosition());
        return ResponseEntity.badRequest().body(body);
    }

    /** Malformed JSON in request body → 400 Bad Request */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex) {
        ErrorResponse body = new ErrorResponse(
                "INVALID_REQUEST",
                "Malformed JSON in request body",
                null);
        return ResponseEntity.badRequest().body(body);
    }

    /** Catch-all for unexpected errors → 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
