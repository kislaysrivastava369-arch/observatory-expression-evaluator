package com.observatory.model;

/**
 * One intermediate calculation step — a sub-expression and its computed result.
 */
public record EvaluationStep(String expression, String result) {}
