package com.observatory.engine;

import com.observatory.exception.EvaluationException;
import com.observatory.model.ASTNode;
import com.observatory.model.Token;
import com.observatory.model.TokenType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser that converts a list of tokens into an AST.
 * <p>
 * Grammar (operator precedence lowest → highest):
 * <pre>
 *   expression  :=  term  ( ("+" | "-") term )*
 *   term        :=  unary ( ("*" | "/") unary )*
 *   unary       :=  ("+" | "-") unary  |  primary
 *   primary     :=  NUMBER  |  IDENTIFIER  |  "(" expression ")"
 * </pre>
 * Addition and subtraction are left-associative at the lowest precedence.
 * Multiplication and division are left-associative at the next level.
 * Unary plus/minus bind tighter than binary operators.
 */
public class Parser {

    private final List<Token> tokens;
    private int pos;
    private final List<String> trace = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    /**
     * Parses the full token stream and returns the root AST node.
     *
     * @throws EvaluationException on syntax errors
     */
    public ASTNode parse() {
        if (tokens.isEmpty()
                || (tokens.size() == 1 && tokens.get(0).type() == TokenType.EOF)) {
            throw new EvaluationException(
                    "EMPTY_EXPRESSION", "Expression is empty", 0);
        }

        trace.add("Begin parsing expression");
        ASTNode result = parseExpression();

        if (current().type() != TokenType.EOF) {
            Token t = current();
            throw new EvaluationException(
                    "UNEXPECTED_TOKEN",
                    String.format("Unexpected token '%s' at position %d after complete expression",
                            t.value(), t.position()),
                    t.position());
        }

        trace.add("Parsing complete — AST built successfully");
        return result;
    }

    /** Returns the parsing trace (human-readable log of parser decisions). */
    public List<String> getTrace() {
        return trace;
    }

    // ── grammar rules ────────────────────────────────────────────────

    /** expression := term (("+" | "-") term)* */
    private ASTNode parseExpression() {
        trace.add(String.format("  Parsing additive expression at position %d", current().position()));
        ASTNode left = parseTerm();

        while (current().type() == TokenType.PLUS || current().type() == TokenType.MINUS) {
            Token op = consume();
            trace.add(String.format("  Binary '%s' — parsing right operand at position %d",
                    op.value(), current().position()));
            ASTNode right = parseTerm();
            left = new ASTNode.BinaryOp(op.value(), left, right, op.position());
        }
        return left;
    }

    /** term := unary (("*" | "/") unary)* */
    private ASTNode parseTerm() {
        trace.add(String.format("  Parsing multiplicative term at position %d", current().position()));
        ASTNode left = parseUnary();

        while (current().type() == TokenType.STAR || current().type() == TokenType.SLASH) {
            Token op = consume();
            trace.add(String.format("  Binary '%s' — parsing right operand at position %d",
                    op.value(), current().position()));
            ASTNode right = parseUnary();
            left = new ASTNode.BinaryOp(op.value(), left, right, op.position());
        }
        return left;
    }

    /** unary := ("+" | "-") unary | primary */
    private ASTNode parseUnary() {
        if (current().type() == TokenType.MINUS) {
            Token op = consume();
            trace.add(String.format("  Unary minus at position %d", op.position()));
            ASTNode operand = parseUnary();
            return new ASTNode.UnaryOp("-", operand, op.position());
        }
        if (current().type() == TokenType.PLUS) {
            Token op = consume();
            trace.add(String.format("  Unary plus (ignored) at position %d", op.position()));
            return parseUnary();
        }
        return parsePrimary();
    }

    /** primary := NUMBER | IDENTIFIER | "(" expression ")" */
    private ASTNode parsePrimary() {
        Token t = current();

        if (t.type() == TokenType.NUMBER) {
            consume();
            trace.add(String.format("  Parsed number literal: %s", t.value()));
            return new ASTNode.NumberLiteral(new BigDecimal(t.value()), t.position());
        }

        if (t.type() == TokenType.IDENTIFIER) {
            consume();
            trace.add(String.format("  Parsed variable reference: %s", t.value()));
            return new ASTNode.Variable(t.value(), t.position());
        }

        if (t.type() == TokenType.LPAREN) {
            consume();
            trace.add(String.format("  Opening '(' at position %d — entering sub-expression", t.position()));
            ASTNode expr = parseExpression();

            if (current().type() != TokenType.RPAREN) {
                throw new EvaluationException(
                        "MISSING_CLOSING_PARENTHESIS",
                        String.format("Missing closing ')' for '(' at position %d", t.position()),
                        t.position());
            }
            consume();
            trace.add(String.format("  Closing ')' matched for '(' at position %d", t.position()));
            return expr;
        }

        if (t.type() == TokenType.RPAREN) {
            throw new EvaluationException(
                    "UNEXPECTED_TOKEN",
                    String.format("Unexpected ')' at position %d — no matching '('", t.position()),
                    t.position());
        }

        if (t.type() == TokenType.EOF) {
            throw new EvaluationException(
                    "UNEXPECTED_END",
                    "Unexpected end of expression — expected a number, variable, or '('",
                    t.position());
        }

        throw new EvaluationException(
                "UNEXPECTED_TOKEN",
                String.format("Unexpected token '%s' at position %d — expected a number, variable, or '('",
                        t.value(), t.position()),
                t.position());
    }

    // ── helpers ──────────────────────────────────────────────────────

    private Token current() {
        return tokens.get(pos);
    }

    private Token consume() {
        Token t = tokens.get(pos);
        if (pos < tokens.size() - 1) pos++;
        return t;
    }
}
