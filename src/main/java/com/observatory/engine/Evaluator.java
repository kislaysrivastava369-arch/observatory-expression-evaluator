package com.observatory.engine;

import com.observatory.exception.EvaluationException;
import com.observatory.model.ASTNode;
import com.observatory.model.EvaluationStep;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recursive AST evaluator using {@link BigDecimal} with
 * {@link MathContext#DECIMAL128} (34 significant digits).
 * <p>
 * Records every intermediate calculation so the user can trace
 * exactly how the final result was obtained.
 */
public class Evaluator {

    /**
     * MathContext used for all arithmetic.
     * DECIMAL128 provides 34 significant digits — more than enough
     * for practical observatory calculations.
     */
    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private final Map<String, BigDecimal> variables;
    private final List<EvaluationStep> steps = new ArrayList<>();

    public Evaluator(Map<String, BigDecimal> variables) {
        this.variables = variables;
    }

    /**
     * Evaluates the AST and returns the final result.
     *
     * @throws EvaluationException on undefined variables or division by zero
     */
    public BigDecimal evaluate(ASTNode node) {
        return switch (node) {

            case ASTNode.NumberLiteral n -> {
                String display = stripTrailing(n.value());
                steps.add(new EvaluationStep(display, display));
                yield n.value();
            }

            case ASTNode.Variable v -> {
                if (!variables.containsKey(v.name())) {
                    throw new EvaluationException(
                            "UNDEFINED_VARIABLE",
                            String.format("Undefined variable '%s' at position %d",
                                    v.name(), v.position()),
                            v.position());
                }
                BigDecimal val = variables.get(v.name());
                steps.add(new EvaluationStep(v.name(), stripTrailing(val)));
                yield val;
            }

            case ASTNode.UnaryOp u -> {
                BigDecimal operand = evaluate(u.operand());
                BigDecimal result = operand.negate(MATH_CONTEXT);
                String expr = String.format("(-(%s))", stripTrailing(operand));
                steps.add(new EvaluationStep(expr, stripTrailing(result)));
                yield result;
            }

            case ASTNode.BinaryOp b -> {
                BigDecimal left = evaluate(b.left());
                BigDecimal right = evaluate(b.right());

                BigDecimal result = switch (b.operator()) {
                    case "+" -> left.add(right, MATH_CONTEXT);
                    case "-" -> left.subtract(right, MATH_CONTEXT);
                    case "*" -> left.multiply(right, MATH_CONTEXT);
                    case "/" -> {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw new EvaluationException(
                                    "DIVISION_BY_ZERO",
                                    String.format("Division by zero at position %d", b.position()),
                                    b.position());
                        }
                        yield left.divide(right, MATH_CONTEXT);
                    }
                    default -> throw new EvaluationException(
                            "UNKNOWN_OPERATOR",
                            String.format("Unknown operator '%s'", b.operator()),
                            b.position());
                };

                String expr = String.format("(%s %s %s)",
                        stripTrailing(left), b.operator(), stripTrailing(right));
                steps.add(new EvaluationStep(expr, stripTrailing(result)));
                yield result;
            }
        };
    }

    /** Returns the ordered list of intermediate calculation steps. */
    public List<EvaluationStep> getSteps() {
        return steps;
    }

    /** Formats a BigDecimal cleanly: strip trailing zeros, use plain notation. */
    private String stripTrailing(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
