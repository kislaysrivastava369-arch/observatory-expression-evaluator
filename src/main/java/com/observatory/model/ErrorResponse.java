package com.observatory.model;

/**
 * Structured JSON error response.
 *
 * <pre>{@code
 * {
 *   "error": {
 *     "code": "MISSING_CLOSING_PARENTHESIS",
 *     "message": "Missing closing parenthesis for '(' at position 4",
 *     "position": 4
 *   }
 * }
 * }</pre>
 */
public class ErrorResponse {

    private ErrorDetail error;

    public ErrorResponse() {}

    public ErrorResponse(String code, String message, Integer position) {
        this.error = new ErrorDetail(code, message, position);
    }

    public ErrorDetail getError() { return error; }
    public void setError(ErrorDetail error) { this.error = error; }

    public record ErrorDetail(String code, String message, Integer position) {}
}
