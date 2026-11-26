package com.observatory.cli;

import com.observatory.exception.EvaluationException;
import com.observatory.model.EvaluateResponse;
import com.observatory.model.EvaluationStep;
import com.observatory.service.ExpressionService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Interactive CLI for The Observatory.
 * Activated with: {@code --spring.profiles.active=cli}
 * <p>
 * Commands:
 * <ul>
 *   <li>{@code set <var> = <value>} — define a variable</li>
 *   <li>{@code vars} — show all defined variables</li>
 *   <li>{@code clear} — clear all variables</li>
 *   <li>{@code eval <expression>} — evaluate an expression</li>
 *   <li>{@code demo} — run the illustrative examples</li>
 *   <li>{@code demo2} — run second demonstration (nested parens, negative values)</li>
 *   <li>{@code error} — demonstrate malformed expression error handling</li>
 *   <li>{@code help} — show available commands</li>
 *   <li>{@code quit / exit} — exit</li>
 * </ul>
 */
@Component
@Profile("cli")
public class CliRunner implements CommandLineRunner {

    private final ExpressionService service;
    private final Map<String, Number> variables = new LinkedHashMap<>();

    public CliRunner(ExpressionService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        printBanner();
        printHelp();

        while (true) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            } else if (line.equalsIgnoreCase("help")) {
                printHelp();
            } else if (line.equalsIgnoreCase("vars")) {
                printVars();
            } else if (line.equalsIgnoreCase("clear")) {
                variables.clear();
                System.out.println("  All variables cleared.");
            } else if (line.equalsIgnoreCase("demo")) {
                runDemo1();
            } else if (line.equalsIgnoreCase("demo2")) {
                runDemo2();
            } else if (line.equalsIgnoreCase("error")) {
                runErrorDemo();
            } else if (line.toLowerCase().startsWith("set ")) {
                handleSet(line.substring(4));
            } else if (line.toLowerCase().startsWith("eval ")) {
                handleEval(line.substring(5));
            } else {
                // Treat bare input as an expression to evaluate
                handleEval(line);
            }
        }
    }

    // ── commands ────────────────────────────────────────────────────

    private void handleSet(String input) {
        String[] parts = input.split("=", 2);
        if (parts.length != 2) {
            System.out.println("  Usage: set <name> = <value>");
            return;
        }
        String name = parts[0].trim();
        String valStr = parts[1].trim();
        try {
            double value = Double.parseDouble(valStr);
            variables.put(name, value);
            System.out.printf("  %s = %s%n", name, valStr);
        } catch (NumberFormatException e) {
            System.out.printf("  Error: '%s' is not a valid number%n", valStr);
        }
    }

    private void handleEval(String expression) {
        try {
            EvaluateResponse resp = service.evaluate(expression, variables);
            printResponse(resp);
        } catch (EvaluationException ex) {
            printError(ex);
        }
    }

    private void runDemo1() {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║  Demo 1: -(a + b) * 2 + 30 / 5             ║");
        System.out.println("║  Variables: a = 10, b = 5                   ║");
        System.out.println("║  Expected result: -24                       ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        Map<String, Number> demoVars = new LinkedHashMap<>();
        demoVars.put("a", 10);
        demoVars.put("b", 5);

        try {
            EvaluateResponse resp = service.evaluate("-(a + b) * 2 + 30 / 5", demoVars);
            printResponse(resp);
        } catch (EvaluationException ex) {
            printError(ex);
        }
    }

    private void runDemo2() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║  Demo 2: -((a + b) * (c - 5)) + 18 / (b - 1)  ║");
        System.out.println("║  Variables: a = -4, b = 3, c = 2               ║");
        System.out.println("║  Expected result: 6                             ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        Map<String, Number> demoVars = new LinkedHashMap<>();
        demoVars.put("a", -4);
        demoVars.put("b", 3);
        demoVars.put("c", 2);

        try {
            EvaluateResponse resp = service.evaluate(
                    "-((a + b) * (c - 5)) + 18 / (b - 1)", demoVars);
            printResponse(resp);
        } catch (EvaluationException ex) {
            printError(ex);
        }
    }

    private void runErrorDemo() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║  Error Demo: a + (b * 2    (missing ')')       ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        Map<String, Number> demoVars = new LinkedHashMap<>();
        demoVars.put("a", 1);
        demoVars.put("b", 2);

        try {
            EvaluateResponse resp = service.evaluate("a + (b * 2", demoVars);
            printResponse(resp);
        } catch (EvaluationException ex) {
            printError(ex);
        }
    }

    // ── display helpers ─────────────────────────────────────────────

    private void printResponse(EvaluateResponse resp) {
        System.out.println();
        System.out.println("═══ Result ═══");
        System.out.println("  " + resp.getResult());

        System.out.println();
        System.out.println("═══ Postfix (Reverse Polish Notation) ═══");
        System.out.println("  " + resp.getPostfix());

        System.out.println();
        System.out.println("═══ Expression Tree ═══");
        for (String line : resp.getTree().split("\n")) {
            System.out.println("  " + line);
        }

        System.out.println();
        System.out.println("═══ Intermediate Calculations ═══");
        int step = 1;
        for (EvaluationStep s : resp.getIntermediateSteps()) {
            System.out.printf("  %2d. %s  →  %s%n", step++, s.expression(), s.result());
        }

        System.out.println();
        System.out.println("═══ Parsing Trace ═══");
        for (String t : resp.getTrace()) {
            System.out.println("  " + t);
        }
    }

    private void printError(EvaluationException ex) {
        System.out.println();
        System.out.println("═══ Error ═══");
        System.out.printf("  Code:     %s%n", ex.getCode());
        System.out.printf("  Message:  %s%n", ex.getMessage());
        if (ex.getPosition() != null) {
            System.out.printf("  Position: %d%n", ex.getPosition());
        }
    }

    private void printVars() {
        if (variables.isEmpty()) {
            System.out.println("  No variables defined.");
            return;
        }
        System.out.println("  Defined variables:");
        for (Map.Entry<String, Number> e : variables.entrySet()) {
            System.out.printf("    %s = %s%n", e.getKey(), e.getValue());
        }
    }

    private void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════╗");
        System.out.println("  ║        🔭  THE OBSERVATORY  🔭           ║");
        System.out.println("  ║   Expression Evaluation & Visualization  ║");
        System.out.println("  ╚═══════════════════════════════════════════╝");
        System.out.println();
    }

    private void printHelp() {
        System.out.println("  Commands:");
        System.out.println("    set <var> = <value>  Define a variable");
        System.out.println("    vars                Show defined variables");
        System.out.println("    clear               Clear all variables");
        System.out.println("    eval <expression>   Evaluate an expression");
        System.out.println("    demo                Run Demo 1 (illustrative example)");
        System.out.println("    demo2               Run Demo 2 (nested parens, negatives)");
        System.out.println("    error               Show malformed-expression error");
        System.out.println("    help                Show this help");
        System.out.println("    quit                Exit");
        System.out.println();
        System.out.println("  Or type an expression directly to evaluate it.");
    }
}
