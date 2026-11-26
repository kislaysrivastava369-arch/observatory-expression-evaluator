package com.observatory.engine;

import com.observatory.exception.EvaluationException;
import com.observatory.model.Token;
import com.observatory.model.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexical analyser (tokenizer) for mathematical expressions.
 * <p>
 * Converts a raw expression string into a list of {@link Token}s.
 * Recognises integers, decimals, identifiers (variable names), the four
 * arithmetic operators (including their Unicode math variants − × ÷),
 * and parentheses.
 * <p>
 * Enforces a hard limit of {@value #MAX_TOKENS} tokens per expression.
 */
public class Tokenizer {

    /** Maximum number of tokens (excluding EOF) allowed in a single expression. */
    public static final int MAX_TOKENS = 2000;

    private final String input;
    private int pos;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> trace = new ArrayList<>();

    public Tokenizer(String input) {
        this.input = input;
        this.pos = 0;
    }

    /**
     * Tokenizes the full input and returns the token list (ending with EOF).
     *
     * @throws EvaluationException on invalid characters or token-limit exceeded
     */
    public List<Token> tokenize() {
        if (input == null || input.isBlank()) {
            tokens.add(new Token(TokenType.EOF, "", 0));
            trace.add("Input is empty — nothing to tokenize");
            return tokens;
        }

        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) break;

            char c = input.charAt(pos);

            if (Character.isDigit(c)) {
                readNumber();
            } else if (Character.isLetter(c) || c == '_') {
                readIdentifier();
            } else if (c == '+') {
                addToken(TokenType.PLUS, "+");
            } else if (c == '-' || c == '\u2212') {
                addToken(TokenType.MINUS, "-");
            } else if (c == '*' || c == '\u00D7') {
                addToken(TokenType.STAR, "*");
            } else if (c == '/' || c == '\u00F7') {
                addToken(TokenType.SLASH, "/");
            } else if (c == '(') {
                addToken(TokenType.LPAREN, "(");
            } else if (c == ')') {
                addToken(TokenType.RPAREN, ")");
            } else {
                throw new EvaluationException(
                        "INVALID_CHARACTER",
                        String.format("Invalid character '%c' (U+%04X) at position %d", c, (int) c, pos),
                        pos);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", pos));

        int tokenCount = tokens.size() - 1; // exclude EOF
        if (tokenCount > MAX_TOKENS) {
            throw new EvaluationException(
                    "TOKEN_LIMIT_EXCEEDED",
                    String.format("Expression has %d tokens, exceeding the maximum of %d",
                            tokenCount, MAX_TOKENS),
                    0);
        }

        trace.add(String.format("Tokenization complete: %d token(s) produced", tokenCount));
        return tokens;
    }

    /** Returns the trace log describing each token that was recognised. */
    public List<String> getTrace() {
        return trace;
    }

    // ── private helpers ──────────────────────────────────────────────

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private void readNumber() {
        int start = pos;
        StringBuilder sb = new StringBuilder();

        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            sb.append(input.charAt(pos++));
        }

        // decimal part
        if (pos < input.length() && input.charAt(pos) == '.') {
            sb.append('.');
            pos++;
            if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) {
                // allow trailing dot, e.g. "5." → 5
            }
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                sb.append(input.charAt(pos++));
            }
        }

        String value = sb.toString();
        tokens.add(new Token(TokenType.NUMBER, value, start));
        trace.add(String.format("Token: NUMBER '%s' at position %d", value, start));
        checkLimit();
    }

    private void readIdentifier() {
        int start = pos;
        StringBuilder sb = new StringBuilder();

        while (pos < input.length()
                && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            sb.append(input.charAt(pos++));
        }

        String name = sb.toString();
        tokens.add(new Token(TokenType.IDENTIFIER, name, start));
        trace.add(String.format("Token: IDENTIFIER '%s' at position %d", name, start));
        checkLimit();
    }

    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, pos));
        trace.add(String.format("Token: %s '%s' at position %d", type, value, pos));
        pos++;
        checkLimit();
    }

    private void checkLimit() {
        if (tokens.size() > MAX_TOKENS) {
            throw new EvaluationException(
                    "TOKEN_LIMIT_EXCEEDED",
                    String.format("Expression exceeds the maximum of %d tokens", MAX_TOKENS),
                    tokens.get(tokens.size() - 1).position());
        }
    }
}
