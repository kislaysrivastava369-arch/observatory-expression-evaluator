package com.observatory.engine;

import com.observatory.exception.EvaluationException;
import com.observatory.model.ASTNode;
import com.observatory.model.EvaluationStep;
import com.observatory.model.Token;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the expression evaluation engine:
 * tokenizer, parser, evaluator, postfix converter, and tree printer.
 */
class ExpressionEngineTest {

    // ════════════════════════════════════════════════════════════════
    //  Tokenizer tests
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tokenizer")
    class TokenizerTests {

        @Test
        @DisplayName("Tokenizes basic arithmetic expression")
        void basicTokenization() {
            Tokenizer t = new Tokenizer("3 + 5 * 2");
            var tokens = t.tokenize();
            // NUMBER PLUS NUMBER STAR NUMBER EOF
            assertEquals(6, tokens.size());
            assertEquals("3", tokens.get(0).value());
            assertEquals("+", tokens.get(1).value());
            assertEquals("5", tokens.get(2).value());
            assertEquals("*", tokens.get(3).value());
            assertEquals("2", tokens.get(4).value());
        }

        @Test
        @DisplayName("Tokenizes identifiers and parentheses")
        void identifiersAndParens() {
            Tokenizer t = new Tokenizer("-(a + b)");
            var tokens = t.tokenize();
            assertEquals(7, tokens.size()); // - ( a + b ) EOF
        }

        @Test
        @DisplayName("Tokenizes decimal numbers")
        void decimalNumbers() {
            Tokenizer t = new Tokenizer("3.14 + 0.5");
            var tokens = t.tokenize();
            assertEquals("3.14", tokens.get(0).value());
            assertEquals("0.5", tokens.get(2).value());
        }

        @Test
        @DisplayName("Accepts Unicode operators: − × ÷")
        void unicodeOperators() {
            Tokenizer t = new Tokenizer("5 \u2212 3 \u00D7 2 \u00F7 1");
            var tokens = t.tokenize();
            assertEquals("-", tokens.get(1).value());
            assertEquals("*", tokens.get(3).value());
            assertEquals("/", tokens.get(5).value());
        }

        @Test
        @DisplayName("Rejects invalid characters")
        void invalidCharacter() {
            Tokenizer t = new Tokenizer("3 + @");
            EvaluationException ex = assertThrows(EvaluationException.class, t::tokenize);
            assertEquals("INVALID_CHARACTER", ex.getCode());
            assertTrue(ex.getMessage().contains("@"));
        }

        @Test
        @DisplayName("Rejects expressions exceeding 2000 tokens")
        void tokenLimitExceeded() {
            // Build an expression with > 2000 tokens: "1+1+1+...+1"
            StringBuilder sb = new StringBuilder("1");
            for (int i = 1; i < 1200; i++) {
                sb.append("+1");
            }
            // This gives 1200 numbers + 1199 operators = 2399 tokens > 2000
            Tokenizer t = new Tokenizer(sb.toString());
            EvaluationException ex = assertThrows(EvaluationException.class, t::tokenize);
            assertEquals("TOKEN_LIMIT_EXCEEDED", ex.getCode());
        }

        @Test
        @DisplayName("Accepts exactly 2000 tokens")
        void tokenLimitExact() {
            // 1000 numbers + 999 operators + 1 number = 1000+999=1999 tokens
            // Actually: "1+1+1...+1" with 1000 ones = 1000 numbers + 999 '+' = 1999 tokens
            StringBuilder sb = new StringBuilder("1");
            for (int i = 1; i < 1000; i++) {
                sb.append("+1");
            }
            // 1000 numbers + 999 plus signs = 1999 tokens <= 2000
            Tokenizer t = new Tokenizer(sb.toString());
            var tokens = t.tokenize();
            assertEquals(2000, tokens.size()); // 1999 + EOF
        }

        @Test
        @DisplayName("Empty input produces only EOF")
        void emptyInput() {
            Tokenizer t = new Tokenizer("");
            var tokens = t.tokenize();
            assertEquals(1, tokens.size());
        }

        @Test
        @DisplayName("Records source positions correctly")
        void sourcePositions() {
            Tokenizer t = new Tokenizer("ab + 12");
            var tokens = t.tokenize();
            assertEquals(0, tokens.get(0).position()); // "ab"
            assertEquals(3, tokens.get(1).position()); // "+"
            assertEquals(5, tokens.get(2).position()); // "12"
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Parser tests
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Parser")
    class ParserTests {

        private ASTNode parse(String expression) {
            Tokenizer tokenizer = new Tokenizer(expression);
            Parser parser = new Parser(tokenizer.tokenize());
            return parser.parse();
        }

        @Test
        @DisplayName("Parses a simple addition")
        void simpleAddition() {
            ASTNode node = parse("3 + 5");
            assertInstanceOf(ASTNode.BinaryOp.class, node);
            ASTNode.BinaryOp op = (ASTNode.BinaryOp) node;
            assertEquals("+", op.operator());
        }

        @Test
        @DisplayName("Respects multiplication precedence over addition")
        void precedence() {
            // 2 + 3 * 4 → ADD(2, MUL(3, 4))
            ASTNode node = parse("2 + 3 * 4");
            assertInstanceOf(ASTNode.BinaryOp.class, node);
            ASTNode.BinaryOp add = (ASTNode.BinaryOp) node;
            assertEquals("+", add.operator());
            assertInstanceOf(ASTNode.NumberLiteral.class, add.left());
            assertInstanceOf(ASTNode.BinaryOp.class, add.right());
            assertEquals("*", ((ASTNode.BinaryOp) add.right()).operator());
        }

        @Test
        @DisplayName("Left-associativity: a - b - c → SUB(SUB(a,b), c)")
        void leftAssociativity() {
            ASTNode node = parse("10 - 3 - 2");
            assertInstanceOf(ASTNode.BinaryOp.class, node);
            ASTNode.BinaryOp outer = (ASTNode.BinaryOp) node;
            assertEquals("-", outer.operator());
            assertInstanceOf(ASTNode.BinaryOp.class, outer.left());
            assertEquals("-", ((ASTNode.BinaryOp) outer.left()).operator());
        }

        @Test
        @DisplayName("Parses unary minus")
        void unaryMinus() {
            ASTNode node = parse("-5");
            assertInstanceOf(ASTNode.UnaryOp.class, node);
            ASTNode.UnaryOp u = (ASTNode.UnaryOp) node;
            assertEquals("-", u.operator());
        }

        @Test
        @DisplayName("Parses nested parentheses")
        void nestedParentheses() {
            ASTNode node = parse("((3 + 2))");
            assertInstanceOf(ASTNode.BinaryOp.class, node);
        }

        @Test
        @DisplayName("Error: missing closing parenthesis")
        void missingClosingParen() {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> parse("a + (b * 2"));
            assertEquals("MISSING_CLOSING_PARENTHESIS", ex.getCode());
            assertTrue(ex.getMessage().contains("Missing closing ')'"));
        }

        @Test
        @DisplayName("Error: unexpected closing parenthesis")
        void unexpectedClosingParen() {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> parse(")"));
            assertEquals("UNEXPECTED_TOKEN", ex.getCode());
        }

        @Test
        @DisplayName("Error: empty expression")
        void emptyExpression() {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> parse(""));
            assertEquals("EMPTY_EXPRESSION", ex.getCode());
        }

        @Test
        @DisplayName("Error: missing operand at end")
        void missingOperand() {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> parse("3 +"));
            assertEquals("UNEXPECTED_END", ex.getCode());
        }

        @Test
        @DisplayName("Parses double unary minus: --5")
        void doubleUnaryMinus() {
            ASTNode node = parse("--5");
            assertInstanceOf(ASTNode.UnaryOp.class, node);
            ASTNode.UnaryOp outer = (ASTNode.UnaryOp) node;
            assertInstanceOf(ASTNode.UnaryOp.class, outer.operand());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Evaluator tests
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Evaluator")
    class EvaluatorTests {

        private BigDecimal eval(String expression, Map<String, BigDecimal> vars) {
            Tokenizer tokenizer = new Tokenizer(expression);
            Parser parser = new Parser(tokenizer.tokenize());
            ASTNode ast = parser.parse();
            Evaluator evaluator = new Evaluator(vars);
            return evaluator.evaluate(ast);
        }

        private BigDecimal eval(String expression) {
            return eval(expression, Map.of());
        }

        @Test
        @DisplayName("Basic arithmetic: 2 + 3 * 4 = 14")
        void basicArithmetic() {
            assertEquals(0, new BigDecimal("14").compareTo(eval("2 + 3 * 4")));
        }

        @Test
        @DisplayName("Subtraction: 10 - 3 - 2 = 5 (left-associative)")
        void leftAssociativeSubtraction() {
            assertEquals(0, new BigDecimal("5").compareTo(eval("10 - 3 - 2")));
        }

        @Test
        @DisplayName("Division: 20 / 4 / 5 = 1 (left-associative)")
        void leftAssociativeDivision() {
            assertEquals(0, new BigDecimal("1").compareTo(eval("20 / 4 / 5")));
        }

        @Test
        @DisplayName("Unary minus: -5 = -5")
        void unaryMinus() {
            assertEquals(0, new BigDecimal("-5").compareTo(eval("-5")));
        }

        @Test
        @DisplayName("Double unary minus: --5 = 5")
        void doubleUnaryMinus() {
            assertEquals(0, new BigDecimal("5").compareTo(eval("--5")));
        }

        @Test
        @DisplayName("Parentheses override precedence: (2 + 3) * 4 = 20")
        void parenthesesOverride() {
            assertEquals(0, new BigDecimal("20").compareTo(eval("(2 + 3) * 4")));
        }

        @Test
        @DisplayName("Nested parentheses: ((2 + 3)) = 5")
        void nestedParens() {
            assertEquals(0, new BigDecimal("5").compareTo(eval("((2 + 3))")));
        }

        @Test
        @DisplayName("Decimal arithmetic: 3.14 * 2 = 6.28")
        void decimalArithmetic() {
            assertEquals(0, new BigDecimal("6.28").compareTo(eval("3.14 * 2")));
        }

        @Test
        @DisplayName("Real division: 7 / 2 = 3.5")
        void realDivision() {
            assertEquals(0, new BigDecimal("3.5").compareTo(eval("7 / 2")));
        }

        @Test
        @DisplayName("Variables: a + b with a=10, b=5 → 15")
        void variables() {
            Map<String, BigDecimal> vars = Map.of(
                    "a", new BigDecimal("10"),
                    "b", new BigDecimal("5"));
            assertEquals(0, new BigDecimal("15").compareTo(eval("a + b", vars)));
        }

        @Test
        @DisplayName("Demo 1: -(a+b)*2+30/5 = -24")
        void demo1() {
            Map<String, BigDecimal> vars = Map.of(
                    "a", new BigDecimal("10"),
                    "b", new BigDecimal("5"));
            assertEquals(0, new BigDecimal("-24").compareTo(
                    eval("-(a + b) * 2 + 30 / 5", vars)));
        }

        @Test
        @DisplayName("Demo 2: -((a+b)*(c-5))+18/(b-1) = 6")
        void demo2() {
            Map<String, BigDecimal> vars = Map.of(
                    "a", new BigDecimal("-4"),
                    "b", new BigDecimal("3"),
                    "c", new BigDecimal("2"));
            assertEquals(0, new BigDecimal("6").compareTo(
                    eval("-((a + b) * (c - 5)) + 18 / (b - 1)", vars)));
        }

        @Test
        @DisplayName("Error: division by zero")
        void divisionByZero() {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> eval("5 / 0"));
            assertEquals("DIVISION_BY_ZERO", ex.getCode());
        }

        @Test
        @DisplayName("Error: undefined variable")
        void undefinedVariable() {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> eval("x + 1"));
            assertEquals("UNDEFINED_VARIABLE", ex.getCode());
            assertTrue(ex.getMessage().contains("x"));
        }

        @Test
        @DisplayName("Intermediate steps are recorded")
        void intermediateSteps() {
            Tokenizer tokenizer = new Tokenizer("3 + 5 * 2");
            Parser parser = new Parser(tokenizer.tokenize());
            ASTNode ast = parser.parse();
            Evaluator evaluator = new Evaluator(Map.of());
            evaluator.evaluate(ast);

            List<EvaluationStep> steps = evaluator.getSteps();
            assertFalse(steps.isEmpty());
            // Last step should be the final result
            assertEquals("13", steps.get(steps.size() - 1).result());
        }

        @Test
        @DisplayName("Complex expression: unary minus with multiplication")
        void unaryMinusMultiply() {
            assertEquals(0, new BigDecimal("-6").compareTo(eval("-2 * 3")));
        }

        @Test
        @DisplayName("Expression: -(3) = -3")
        void unaryMinusParens() {
            assertEquals(0, new BigDecimal("-3").compareTo(eval("-(3)")));
        }

        @Test
        @DisplayName("Expression with negative variable values")
        void negativeVariables() {
            Map<String, BigDecimal> vars = Map.of("x", new BigDecimal("-10"));
            assertEquals(0, new BigDecimal("-10").compareTo(eval("x", vars)));
        }

        @Test
        @DisplayName("Mixed precedence: 2 + 3 * 4 - 1 = 13")
        void mixedPrecedence() {
            assertEquals(0, new BigDecimal("13").compareTo(eval("2 + 3 * 4 - 1")));
        }

        @Test
        @DisplayName("Division by variable that is zero")
        void divisionByZeroVariable() {
            Map<String, BigDecimal> vars = Map.of("z", BigDecimal.ZERO);
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> eval("10 / z", vars));
            assertEquals("DIVISION_BY_ZERO", ex.getCode());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Postfix converter tests
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PostfixConverter")
    class PostfixConverterTests {

        private String toPostfix(String expression) {
            Tokenizer tokenizer = new Tokenizer(expression);
            Parser parser = new Parser(tokenizer.tokenize());
            ASTNode ast = parser.parse();
            return new PostfixConverter().convert(ast);
        }

        @Test
        @DisplayName("Simple: 3 + 5 → '3 5 +'")
        void simplePostfix() {
            assertEquals("3 5 +", toPostfix("3 + 5"));
        }

        @Test
        @DisplayName("Precedence: 2 + 3 * 4 → '2 3 4 * +'")
        void precedencePostfix() {
            assertEquals("2 3 4 * +", toPostfix("2 + 3 * 4"));
        }

        @Test
        @DisplayName("Unary minus: -5 → '5 NEG'")
        void unaryPostfix() {
            assertEquals("5 NEG", toPostfix("-5"));
        }

        @Test
        @DisplayName("Demo 1 postfix")
        void demo1Postfix() {
            assertEquals("a b + NEG 2 * 30 5 / +", toPostfix("-(a + b) * 2 + 30 / 5"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Tree printer tests
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TreePrinter")
    class TreePrinterTests {

        @Test
        @DisplayName("Prints a readable tree for simple expression")
        void simpleTree() {
            Tokenizer tokenizer = new Tokenizer("3 + 5");
            Parser parser = new Parser(tokenizer.tokenize());
            ASTNode ast = parser.parse();
            String tree = new TreePrinter().print(ast);
            assertTrue(tree.contains("ADD (+)"));
            assertTrue(tree.contains("3"));
            assertTrue(tree.contains("5"));
        }

        @Test
        @DisplayName("Prints NEGATE for unary minus")
        void unaryTree() {
            Tokenizer tokenizer = new Tokenizer("-x");
            Parser parser = new Parser(tokenizer.tokenize());
            ASTNode ast = parser.parse();
            String tree = new TreePrinter().print(ast);
            assertTrue(tree.contains("NEGATE"));
            assertTrue(tree.contains("x"));
        }
    }
}
