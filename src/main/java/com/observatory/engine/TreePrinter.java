package com.observatory.engine;

import com.observatory.model.ASTNode;

/**
 * Renders an AST as a readable, indented text tree using box-drawing characters.
 *
 * <p>Example output:
 * <pre>
 * └── ADD (+)
 *     ├── MULTIPLY (*)
 *     │   ├── NEGATE
 *     │   │   └── ADD (+)
 *     │   │       ├── a
 *     │   │       └── b
 *     │   └── 2
 *     └── DIVIDE (/)
 *         ├── 30
 *         └── 5
 * </pre>
 */
public class TreePrinter {

    /**
     * Returns the full tree string for the given AST.
     */
    public String print(ASTNode node) {
        StringBuilder sb = new StringBuilder();
        printNode(node, sb, "", true);
        return sb.toString().stripTrailing();
    }

    private void printNode(ASTNode node, StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "\u2514\u2500\u2500 " : "\u251C\u2500\u2500 "); // └── or ├──

        switch (node) {
            case ASTNode.NumberLiteral n ->
                    sb.append(n.value().stripTrailingZeros().toPlainString()).append('\n');

            case ASTNode.Variable v ->
                    sb.append(v.name()).append('\n');

            case ASTNode.UnaryOp u -> {
                sb.append("NEGATE").append('\n');
                String childPrefix = prefix + (isLast ? "    " : "\u2502   "); // │
                printNode(u.operand(), sb, childPrefix, true);
            }

            case ASTNode.BinaryOp b -> {
                sb.append(operatorLabel(b.operator())).append('\n');
                String childPrefix = prefix + (isLast ? "    " : "\u2502   ");
                printNode(b.left(), sb, childPrefix, false);
                printNode(b.right(), sb, childPrefix, true);
            }
        }
    }

    private String operatorLabel(String op) {
        return switch (op) {
            case "+" -> "ADD (+)";
            case "-" -> "SUBTRACT (-)";
            case "*" -> "MULTIPLY (*)";
            case "/" -> "DIVIDE (/)";
            default  -> op;
        };
    }
}
