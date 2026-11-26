package com.observatory.model;

import java.math.BigDecimal;

/**
 * Sealed AST (Abstract Syntax Tree) hierarchy for parsed expressions.
 * Each node carries its source position for error reporting.
 */
public sealed interface ASTNode {

    /** 0-based character position in the original expression. */
    int position();

    /** A numeric literal (integer or decimal). */
    record NumberLiteral(BigDecimal value, int position) implements ASTNode {}

    /** A named variable reference. */
    record Variable(String name, int position) implements ASTNode {}

    /** A unary operator (only unary minus is supported). */
    record UnaryOp(String operator, ASTNode operand, int position) implements ASTNode {}

    /** A binary operator (+, -, *, /). */
    record BinaryOp(String operator, ASTNode left, ASTNode right, int position) implements ASTNode {}
}
