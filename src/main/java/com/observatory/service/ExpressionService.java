package com.observatory.service;

import com.observatory.engine.*;
import com.observatory.model.*;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full expression evaluation pipeline:
 * tokenize → parse → evaluate, and collects the postfix string,
 * expression tree, trace log, and intermediate steps.
 */
@Service
public class ExpressionService {

    /**
     * Evaluates the given request and returns a fully populated response.
     */
    public EvaluateResponse evaluate(EvaluateRequest request) {
        return evaluate(request.getExpression(), request.getVariables());
    }

    /**
     * Core evaluation method used by both the API controller and the CLI.
     */
    public EvaluateResponse evaluate(String expression, Map<String, Number> rawVariables) {
        // Convert variable values to BigDecimal
        Map<String, BigDecimal> variables = convertVariables(rawVariables);

        // 1. Tokenize
        Tokenizer tokenizer = new Tokenizer(expression);
        List<Token> tokens = tokenizer.tokenize();

        // 2. Parse → AST
        Parser parser = new Parser(tokens);
        ASTNode ast = parser.parse();

        // 3. Evaluate with intermediate step tracking
        Evaluator evaluator = new Evaluator(variables);
        BigDecimal result = evaluator.evaluate(ast);

        // 4. Generate postfix notation
        PostfixConverter postfixConverter = new PostfixConverter();
        String postfix = postfixConverter.convert(ast);

        // 5. Generate readable expression tree
        TreePrinter treePrinter = new TreePrinter();
        String tree = treePrinter.print(ast);

        // 6. Merge traces
        List<String> trace = new ArrayList<>();
        trace.addAll(tokenizer.getTrace());
        trace.addAll(parser.getTrace());

        // 7. Build response
        String resultStr = result.stripTrailingZeros().toPlainString();
        return new EvaluateResponse(resultStr, postfix, tree, trace, evaluator.getSteps());
    }

    private Map<String, BigDecimal> convertVariables(Map<String, Number> raw) {
        Map<String, BigDecimal> vars = new LinkedHashMap<>();
        if (raw == null) return vars;

        for (Map.Entry<String, Number> entry : raw.entrySet()) {
            String name = entry.getKey();
            Number value = entry.getValue();
            if (value == null) {
                throw new com.observatory.exception.EvaluationException(
                        "INVALID_VARIABLE_VALUE",
                        String.format("Variable '%s' has a null value", name),
                        null);
            }
            vars.put(name, new BigDecimal(value.toString()));
        }
        return vars;
    }
}
