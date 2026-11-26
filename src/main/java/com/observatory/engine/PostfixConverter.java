package com.observatory.engine;

import com.observatory.model.ASTNode;

/**
 * Converts an AST to its postfix (Reverse Polish Notation) string.
 * Unary minus is represented as {@code NEG} to distinguish it
 * from binary subtraction.
 */
public class PostfixConverter {

    /**
     * Walks the AST and returns the postfix expression string.
     */
    public String convert(ASTNode node) {
        return switch (node) {
            case ASTNode.NumberLiteral n ->
                    n.value().stripTrailingZeros().toPlainString();

            case ASTNode.Variable v ->
                    v.name();

            case ASTNode.UnaryOp u ->
                    convert(u.operand()) + " NEG";

            case ASTNode.BinaryOp b ->
                    convert(b.left()) + " " + convert(b.right()) + " " + b.operator();
        };
    }
}
