package com.observatory.model;

import java.util.List;

/**
 * Successful JSON response from POST /api/evaluate.
 *
 * <pre>{@code
 * {
 *   "result": "-24",
 *   "postfix": "a b + NEG 2 * 30 5 / +",
 *   "tree": "└── ADD (+)\n    ├── ...",
 *   "trace": [ "Token: ...", "Parsing: ..." ],
 *   "intermediateSteps": [
 *     { "expression": "(10 + 5)", "result": "15" },
 *     ...
 *   ]
 * }
 * }</pre>
 */
public class EvaluateResponse {

    private String result;
    private String postfix;
    private String tree;
    private List<String> trace;
    private List<EvaluationStep> intermediateSteps;

    public EvaluateResponse() {}

    public EvaluateResponse(String result, String postfix, String tree,
                            List<String> trace, List<EvaluationStep> intermediateSteps) {
        this.result = result;
        this.postfix = postfix;
        this.tree = tree;
        this.trace = trace;
        this.intermediateSteps = intermediateSteps;
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getPostfix() { return postfix; }
    public void setPostfix(String postfix) { this.postfix = postfix; }

    public String getTree() { return tree; }
    public void setTree(String tree) { this.tree = tree; }

    public List<String> getTrace() { return trace; }
    public void setTrace(List<String> trace) { this.trace = trace; }

    public List<EvaluationStep> getIntermediateSteps() { return intermediateSteps; }
    public void setIntermediateSteps(List<EvaluationStep> intermediateSteps) { this.intermediateSteps = intermediateSteps; }
}
