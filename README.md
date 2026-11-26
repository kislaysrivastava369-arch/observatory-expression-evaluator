# 🔭 The Observatory

**Expression Evaluation and Visualization System**

At a small observatory, volunteers use mathematical expressions to calculate nightly measurements. This system produces both an **accurate result** and an **understandable record** of how it was obtained — including tokenization trace, postfix notation, intermediate calculations, and a readable expression tree.

---

## ✅ Features

- **Custom tokenizer, recursive-descent parser, and AST evaluator** — no `eval()` or scripting libraries
- **BigDecimal arithmetic** (DECIMAL128 — 34 significant digits) for precise calculations
- **Full evaluation trace**: tokenization log, parsing trace, postfix (RPN), intermediate steps, expression tree
- **Robust error handling**: invalid characters, missing brackets, undefined variables, division by zero, token limit
- **REST API** (`POST /api/evaluate`) with structured JSON responses
- **Interactive CLI** with demo examples built in
- **CORS configuration** via environment variable for separate frontend deployment
- **Up to 2,000 tokens** per expression

## 📋 Prerequisites

- **Java 21** (JDK) — [Download Temurin](https://adoptium.net/)
- **Maven 3.9+** — [Download Maven](https://maven.apache.org/download.cgi)

Verify your installation:
```bash
java --version    # Should show 21+
mvn --version     # Should show 3.9+
```

> **No Maven?** Install it quickly:
> ```
> # Windows (winget)
> winget install Apache.Maven
>
> # Or download from https://maven.apache.org/download.cgi
> # Extract, add bin/ to PATH
> ```

---

## 🚀 Quick Start

### 1. Run Tests
```bash
cd DSAHACK
mvn clean test
```

### 2. Start the API Server
```bash
mvn spring-boot:run
```
The server starts at **http://localhost:8080**.

### 3. Start the Interactive CLI
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=cli
```

### 4. Build a Deployable JAR
```bash
mvn clean package -DskipTests
java -jar target/observatory-1.0.0.jar
```

---

## 🖥️ Interactive CLI

Start CLI mode and try the built-in demos:

```
mvn spring-boot:run -Dspring-boot.run.profiles=cli
```

```
  ╔═══════════════════════════════════════════╗
  ║        🔭  THE OBSERVATORY  🔭           ║
  ║   Expression Evaluation & Visualization  ║
  ╚═══════════════════════════════════════════╝

  Commands:
    set <var> = <value>  Define a variable
    vars                Show defined variables
    clear               Clear all variables
    eval <expression>   Evaluate an expression
    demo                Run Demo 1 (illustrative example)
    demo2               Run Demo 2 (nested parens, negatives)
    error               Show malformed-expression error
    help                Show this help
    quit                Exit

> demo
```

---

## 📡 API Usage

### Evaluate an Expression

```http
POST http://localhost:8080/api/evaluate
Content-Type: application/json
```

#### Request
```json
{
  "expression": "-(a + b) * 2 + 30 / 5",
  "variables": {
    "a": 10,
    "b": 5
  }
}
```

#### Successful Response (200 OK)
```json
{
  "result": "-24",
  "postfix": "a b + NEG 2 * 30 5 / +",
  "tree": "└── ADD (+)\n    ├── MULTIPLY (*)\n    │   ├── NEGATE\n    │   │   └── ADD (+)\n    │   │       ├── a\n    │   │       └── b\n    │   └── 2\n    └── DIVIDE (/)\n        ├── 30\n        └── 5",
  "trace": [
    "Token: MINUS '-' at position 0",
    "Token: LPAREN '(' at position 1",
    "Token: IDENTIFIER 'a' at position 2",
    "..."
  ],
  "intermediateSteps": [
    { "expression": "a", "result": "10" },
    { "expression": "b", "result": "5" },
    { "expression": "(10 + 5)", "result": "15" },
    { "expression": "(-(15))", "result": "-15" },
    { "expression": "2", "result": "2" },
    { "expression": "(-15 * 2)", "result": "-30" },
    { "expression": "30", "result": "30" },
    { "expression": "5", "result": "5" },
    { "expression": "(30 / 5)", "result": "6" },
    { "expression": "(-30 + 6)", "result": "-24" }
  ]
}
```

#### Error Response (400 Bad Request)
```json
{
  "error": {
    "code": "MISSING_CLOSING_PARENTHESIS",
    "message": "Missing closing ')' for '(' at position 4",
    "position": 4
  }
}
```

### Test with curl

```bash
# Demo 1
curl -X POST http://localhost:8080/api/evaluate \
  -H "Content-Type: application/json" \
  -d '{"expression":"-(a + b) * 2 + 30 / 5","variables":{"a":10,"b":5}}'

# Demo 2
curl -X POST http://localhost:8080/api/evaluate \
  -H "Content-Type: application/json" \
  -d '{"expression":"-((a + b) * (c - 5)) + 18 / (b - 1)","variables":{"a":-4,"b":3,"c":2}}'

# Malformed expression (error demo)
curl -X POST http://localhost:8080/api/evaluate \
  -H "Content-Type: application/json" \
  -d '{"expression":"a + (b * 2","variables":{"a":1,"b":2}}'

# Health check
curl http://localhost:8080/api/health
```

### PowerShell (Windows)

```powershell
# Demo 1
Invoke-RestMethod -Uri http://localhost:8080/api/evaluate -Method POST `
  -ContentType "application/json" `
  -Body '{"expression":"-(a + b) * 2 + 30 / 5","variables":{"a":10,"b":5}}' | ConvertTo-Json -Depth 5

# Demo 2
Invoke-RestMethod -Uri http://localhost:8080/api/evaluate -Method POST `
  -ContentType "application/json" `
  -Body '{"expression":"-((a + b) * (c - 5)) + 18 / (b - 1)","variables":{"a":-4,"b":3,"c":2}}' | ConvertTo-Json -Depth 5
```

---

## 🧪 Demonstrations

### Demo 1 — Illustrative Example
| | |
|---|---|
| **Variables** | a = 10, b = 5 |
| **Expression** | `-(a + b) * 2 + 30 / 5` |
| **Expected** | **-24** |

### Demo 2 — Nested Parentheses & Negative Values
| | |
|---|---|
| **Variables** | a = -4, b = 3, c = 2 |
| **Expression** | `-((a + b) * (c - 5)) + 18 / (b - 1)` |
| **Expected** | **6** |

### Error Demo — Malformed Expression
| | |
|---|---|
| **Expression** | `a + (b * 2` |
| **Error** | `MISSING_CLOSING_PARENTHESIS` — Missing closing ')' for '(' at position 4 |

---

## 🏗️ Project Structure

```
DSAHACK/
├── pom.xml                                    # Maven configuration
├── README.md                                  # This file
├── mvnw.cmd                                   # Maven Wrapper (Windows)
├── src/main/java/com/observatory/
│   ├── ObservatoryApplication.java            # Spring Boot entry point
│   ├── model/
│   │   ├── TokenType.java                     # Token type enum
│   │   ├── Token.java                         # Lexical token (type, value, position)
│   │   ├── ASTNode.java                       # Sealed AST hierarchy
│   │   ├── EvaluateRequest.java               # API request DTO
│   │   ├── EvaluateResponse.java              # API response DTO
│   │   ├── EvaluationStep.java                # Intermediate calculation step
│   │   └── ErrorResponse.java                 # Structured error DTO
│   ├── engine/
│   │   ├── Tokenizer.java                     # Lexical analyser
│   │   ├── Parser.java                        # Recursive-descent parser
│   │   ├── Evaluator.java                     # AST evaluator (BigDecimal)
│   │   ├── PostfixConverter.java              # AST → postfix string
│   │   └── TreePrinter.java                   # AST → indented text tree
│   ├── service/
│   │   └── ExpressionService.java             # Orchestrates the pipeline
│   ├── controller/
│   │   ├── EvaluateController.java            # POST /api/evaluate
│   │   └── HealthController.java              # GET /api/health
│   ├── config/
│   │   └── CorsConfig.java                    # CORS configuration
│   ├── exception/
│   │   ├── EvaluationException.java           # Domain exception
│   │   └── GlobalExceptionHandler.java        # → JSON error responses
│   └── cli/
│       └── CliRunner.java                     # Interactive terminal mode
├── src/main/resources/
│   ├── application.properties                 # Server config
│   └── application-cli.properties             # CLI profile config
└── src/test/java/com/observatory/
    ├── engine/ExpressionEngineTest.java        # 25+ unit tests
    └── controller/EvaluateControllerTest.java  # 12 API integration tests
```

---

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP server port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated frontend origins |

### For Frontend Integration

The frontend should set its API base URL to wherever this backend is deployed:
```
VITE_API_BASE_URL=http://localhost:8080
```

### For Production Deployment

```bash
# Set CORS for your frontend domain
export CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
export SERVER_PORT=8080

java -jar target/observatory-1.0.0.jar
```

---

## 📐 Grammar

```
expression  :=  term  ( ("+" | "-") term )*
term        :=  unary ( ("*" | "/") unary )*
unary       :=  ("+" | "-") unary  |  primary
primary     :=  NUMBER  |  IDENTIFIER  |  "(" expression ")"
```

- **Precedence** (lowest → highest): addition/subtraction → multiplication/division → unary → primary
- **Associativity**: all binary operators are left-associative
- **Arithmetic**: `BigDecimal` with `MathContext.DECIMAL128` (34 significant digits)

---

## 📝 Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| `EMPTY_EXPRESSION` | 400 | Expression string is empty or blank |
| `INVALID_CHARACTER` | 400 | Unsupported character in expression |
| `TOKEN_LIMIT_EXCEEDED` | 400 | Expression exceeds 2,000 tokens |
| `MISSING_CLOSING_PARENTHESIS` | 400 | Unmatched opening `(` |
| `UNEXPECTED_TOKEN` | 400 | Token in wrong position |
| `UNEXPECTED_END` | 400 | Expression ends prematurely |
| `UNDEFINED_VARIABLE` | 400 | Variable not in provided map |
| `DIVISION_BY_ZERO` | 400 | Divisor evaluates to zero |
| `INVALID_VARIABLE_VALUE` | 400 | Null variable value |
| `INVALID_REQUEST` | 400 | Malformed JSON body |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 🚢 Deployment

### As a JAR (any cloud VM / VPS)
```bash
mvn clean package
scp target/observatory-1.0.0.jar user@server:/app/
# On the server:
CORS_ALLOWED_ORIGINS=https://frontend.example.com java -jar /app/observatory-1.0.0.jar
```

### Docker (optional)
```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/observatory-1.0.0.jar /app/observatory.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/observatory.jar"]
```

---

## 🔑 Push to GitHub

```bash
cd DSAHACK
git init
git add .
git commit -m "The Observatory — Expression Evaluation and Visualization System"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/observatory.git
git push -u origin main
```

---

## 📄 License

MIT
