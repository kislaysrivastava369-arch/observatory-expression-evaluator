package com.observatory.controller;

import com.observatory.model.EvaluateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the REST API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EvaluateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/evaluate — Demo 1 returns -24")
    void demo1() throws Exception {
        String json = """
                {
                  "expression": "-(a + b) * 2 + 30 / 5",
                  "variables": { "a": 10, "b": 5 }
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("-24"))
                .andExpect(jsonPath("$.postfix").value("a b + NEG 2 * 30 5 / +"))
                .andExpect(jsonPath("$.tree").isNotEmpty())
                .andExpect(jsonPath("$.trace").isArray())
                .andExpect(jsonPath("$.intermediateSteps").isArray());
    }

    @Test
    @DisplayName("POST /api/evaluate — Demo 2 returns 6")
    void demo2() throws Exception {
        String json = """
                {
                  "expression": "-((a + b) * (c - 5)) + 18 / (b - 1)",
                  "variables": { "a": -4, "b": 3, "c": 2 }
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("6"));
    }

    @Test
    @DisplayName("POST /api/evaluate — simple arithmetic without variables")
    void simpleArithmetic() throws Exception {
        String json = """
                {
                  "expression": "2 + 3 * 4",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("14"));
    }

    @Test
    @DisplayName("POST /api/evaluate — missing closing parenthesis returns 400")
    void missingClosingParen() throws Exception {
        String json = """
                {
                  "expression": "a + (b * 2",
                  "variables": { "a": 1, "b": 2 }
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_CLOSING_PARENTHESIS"))
                .andExpect(jsonPath("$.error.message").value(containsString("Missing closing ')'")))
                .andExpect(jsonPath("$.error.position").isNumber());
    }

    @Test
    @DisplayName("POST /api/evaluate — undefined variable returns 400")
    void undefinedVariable() throws Exception {
        String json = """
                {
                  "expression": "x + 1",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNDEFINED_VARIABLE"))
                .andExpect(jsonPath("$.error.message").value(containsString("x")));
    }

    @Test
    @DisplayName("POST /api/evaluate — division by zero returns 400")
    void divisionByZero() throws Exception {
        String json = """
                {
                  "expression": "5 / 0",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DIVISION_BY_ZERO"));
    }

    @Test
    @DisplayName("POST /api/evaluate — empty expression returns 400")
    void emptyExpression() throws Exception {
        String json = """
                {
                  "expression": "",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EMPTY_EXPRESSION"));
    }

    @Test
    @DisplayName("POST /api/evaluate — invalid character returns 400")
    void invalidCharacter() throws Exception {
        String json = """
                {
                  "expression": "3 + @",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CHARACTER"));
    }

    @Test
    @DisplayName("POST /api/evaluate — malformed JSON returns 400")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("GET /api/health — returns UP")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("The Observatory"));
    }

    @Test
    @DisplayName("POST /api/evaluate — response includes all required fields")
    void responseStructure() throws Exception {
        String json = """
                {
                  "expression": "1 + 2",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.postfix").exists())
                .andExpect(jsonPath("$.tree").exists())
                .andExpect(jsonPath("$.trace").isArray())
                .andExpect(jsonPath("$.intermediateSteps").isArray())
                .andExpect(jsonPath("$.intermediateSteps[0].expression").exists())
                .andExpect(jsonPath("$.intermediateSteps[0].result").exists());
    }

    @Test
    @DisplayName("POST /api/evaluate — decimal division")
    void decimalDivision() throws Exception {
        String json = """
                {
                  "expression": "7 / 2",
                  "variables": {}
                }
                """;

        mockMvc.perform(post("/api/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("3.5"));
    }
}
