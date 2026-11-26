package com.observatory.model;

/**
 * A single lexical token with its type, text value, and 0-based source position.
 */
public record Token(TokenType type, String value, int position) {

    @Override
    public String toString() {
        return String.format("%s '%s' @ %d", type, value, position);
    }
}
