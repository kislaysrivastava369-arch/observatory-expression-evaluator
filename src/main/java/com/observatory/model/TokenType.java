package com.observatory.model;

/**
 * Types of lexical tokens recognised by the expression tokenizer.
 */
public enum TokenType {
    NUMBER,       // integer or decimal literal
    IDENTIFIER,   // variable name
    PLUS,         // +
    MINUS,        // - or − (U+2212)
    STAR,         // * or × (U+00D7)
    SLASH,        // / or ÷ (U+00F7)
    LPAREN,       // (
    RPAREN,       // )
    EOF           // end of input
}
