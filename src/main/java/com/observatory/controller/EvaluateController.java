package com.observatory.controller;

import com.observatory.model.EvaluateRequest;
import com.observatory.model.EvaluateResponse;
import com.observatory.service.ExpressionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the expression evaluation API.
 *
 * <pre>
 * POST /api/evaluate
 * Content-Type: application/json
 *
 * { "expression": "-(a + b) * 2 + 30 / 5", "variables": { "a": 10, "b": 5 } }
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class EvaluateController {

    private final ExpressionService expressionService;

    public EvaluateController(ExpressionService expressionService) {
        this.expressionService = expressionService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(@RequestBody EvaluateRequest request) {
        EvaluateResponse response = expressionService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}
