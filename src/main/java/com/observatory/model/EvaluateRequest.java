package com.observatory.model;

import java.util.Map;

/**
 * Incoming JSON request body for POST /api/evaluate.
 *
 * <pre>{@code
 * {
 *   "expression": "-(a + b) * 2 + 30 / 5",
 *   "variables": { "a": 10, "b": 5 }
 * }
 * }</pre>
 */
public class EvaluateRequest {

    private String expression;
    private Map<String, Number> variables;

    public EvaluateRequest() {}

    public EvaluateRequest(String expression, Map<String, Number> variables) {
        this.expression = expression;
        this.variables = variables;
    }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public Map<String, Number> getVariables() { return variables; }
    public void setVariables(Map<String, Number> variables) { this.variables = variables; }
}
