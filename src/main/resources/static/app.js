/**
 * The Observatory — Frontend Controller
 *
 * Strictly delegates all expression tokenization, parsing, AST construction,
 * and evaluation to the Java Spring Boot backend (`POST /api/evaluate`).
 * Never evaluates expressions locally.
 */

(() => {
  const defaultBase =
    window.location.protocol === "file:" ? "http://localhost:8080" : "";

  let apiBaseUrl = localStorage.getItem("observatory_api_base") ?? defaultBase;

  // ── DOM Elements ───────────────────────────────────────────────
  const healthBadge = document.getElementById("health-badge");
  const healthText = document.getElementById("health-text");
  const toggleSettingsBtn = document.getElementById("toggle-settings-btn");
  const settingsPanel = document.getElementById("api-settings-panel");
  const apiBaseInput = document.getElementById("api-base-input");
  const saveApiBtn = document.getElementById("save-api-btn");

  const evaluateForm = document.getElementById("evaluate-form");
  const expressionInput = document.getElementById("expression-input");
  const variablesList = document.getElementById("variables-list");
  const noVarsMsg = document.getElementById("no-vars-msg");
  const addVarBtn = document.getElementById("add-var-btn");
  const evaluateBtn = document.getElementById("evaluate-btn");
  const clearBtn = document.getElementById("clear-btn");

  const btnDemo1 = document.getElementById("btn-demo-1");
  const btnDemo2 = document.getElementById("btn-demo-2");
  const btnDemoErr = document.getElementById("btn-demo-err");

  const stateInitial = document.getElementById("state-initial");
  const stateLoading = document.getElementById("state-loading");
  const stateError = document.getElementById("state-error");
  const stateSuccess = document.getElementById("state-success");

  const errorCodeBadge = document.getElementById("error-code-badge");
  const errorMessageEl = document.getElementById("error-message");
  const errorPointerBox = document.getElementById("error-pointer-box");
  const errorPointerPre = document.getElementById("error-pointer-pre");

  const resultValueEl = document.getElementById("result-value");
  const postfixValueEl = document.getElementById("postfix-value");
  const stepsListEl = document.getElementById("steps-list");
  const treeOutputEl = document.getElementById("tree-output");
  const traceListEl = document.getElementById("trace-list");

  const tabButtons = document.querySelectorAll(".tab-btn");
  const tabPanels = {
    steps: document.getElementById("tab-steps"),
    tree: document.getElementById("tab-tree"),
    trace: document.getElementById("tab-trace"),
  };

  // ── Variable State ─────────────────────────────────────────────
  let variables = [
    { name: "a", value: "10" },
    { name: "b", value: "5" },
  ];

  function renderVariables() {
    variablesList.innerHTML = "";
    noVarsMsg.classList.toggle("hidden", variables.length > 0);

    variables.forEach((v, idx) => {
      const row = document.createElement("div");
      row.className = "var-row";

      const nameInput = document.createElement("input");
      nameInput.type = "text";
      nameInput.placeholder = "name (e.g. a)";
      nameInput.value = v.name;
      nameInput.setAttribute("aria-label", `Variable ${idx + 1} name`);
      nameInput.addEventListener("input", (e) => {
        variables[idx].name = e.target.value;
      });

      const eq = document.createElement("span");
      eq.className = "var-eq";
      eq.textContent = "=";
      eq.setAttribute("aria-hidden", "true");

      const valInput = document.createElement("input");
      valInput.type = "number";
      valInput.step = "any";
      valInput.placeholder = "value";
      valInput.value = v.value;
      valInput.setAttribute("aria-label", `Variable ${idx + 1} value`);
      valInput.addEventListener("input", (e) => {
        variables[idx].value = e.target.value;
      });

      const removeBtn = document.createElement("button");
      removeBtn.type = "button";
      removeBtn.className = "btn-danger-icon";
      removeBtn.textContent = "×";
      removeBtn.title = "Remove variable";
      removeBtn.setAttribute("aria-label", `Remove variable ${v.name || idx + 1}`);
      removeBtn.addEventListener("click", () => {
        variables.splice(idx, 1);
        renderVariables();
      });

      row.append(nameInput, eq, valInput, removeBtn);
      variablesList.appendChild(row);
    });
  }

  // ── View State Management ──────────────────────────────────────
  function setViewState(mode) {
    stateInitial.classList.toggle("hidden", mode !== "initial");
    stateLoading.classList.toggle("hidden", mode !== "loading");
    stateError.classList.toggle("hidden", mode !== "error");
    stateSuccess.classList.toggle("hidden", mode !== "success");
    evaluateBtn.disabled = mode === "loading";
  }

  function showError(code, message, position, sourceExpression) {
    errorCodeBadge.textContent = code || "ERROR";
    errorMessageEl.textContent = message || "An unexpected error occurred.";

    if (typeof position === "number" && sourceExpression && !sourceExpression.includes("\n")) {
      const pointer = " ".repeat(Math.max(0, position)) + "^";
      errorPointerPre.textContent = `${sourceExpression}\n${pointer} (position ${position})`;
      errorPointerBox.classList.remove("hidden");
    } else {
      errorPointerBox.classList.add("hidden");
    }

    setViewState("error");
  }

  function showSuccess(data) {
    resultValueEl.textContent = data.result ?? "—";
    postfixValueEl.textContent = data.postfix ?? "—";
    treeOutputEl.textContent = data.tree ?? "";

    stepsListEl.innerHTML = "";
    (data.intermediateSteps || []).forEach((step) => {
      const li = document.createElement("li");
      const exprSpan = document.createElement("span");
      exprSpan.textContent = step.expression;

      const arrow = document.createElement("span");
      arrow.className = "step-arrow";
      arrow.textContent = "→";

      const resSpan = document.createElement("span");
      resSpan.className = "step-res";
      resSpan.textContent = step.result;

      li.append(exprSpan, arrow, resSpan);
      stepsListEl.appendChild(li);
    });

    traceListEl.innerHTML = "";
    (data.trace || []).forEach((line) => {
      const li = document.createElement("li");
      li.textContent = line;
      traceListEl.appendChild(li);
    });

    setViewState("success");
  }

  // ── API Communication ──────────────────────────────────────────
  function buildUrl(path) {
    const cleanBase = (apiBaseUrl || "").trim().replace(/\/+$/, "");
    return `${cleanBase}${path}`;
  }

  async function checkHealth() {
    healthBadge.className = "health-badge checking";
    healthText.textContent = "Checking API…";
    try {
      const res = await fetch(buildUrl("/api/health"));
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const json = await res.json();
      if (json.status === "UP") {
        healthBadge.className = "health-badge up";
        healthText.textContent = "API Connected";
      } else {
        throw new Error("Unexpected health payload");
      }
    } catch {
      healthBadge.className = "health-badge down";
      healthText.textContent = "API Unreachable";
    }
  }

  async function handleEvaluate(event) {
    if (event) event.preventDefault();

    const expression = expressionInput.value;
    const varMap = {};

    for (const entry of variables) {
      const trimmedName = entry.name.trim();
      const trimmedVal = String(entry.value).trim();
      if (!trimmedName && !trimmedVal) continue;

      if (!trimmedName) {
        showError("INVALID_VARIABLE_NAME", "Each variable must have a name.", null, expression);
        return;
      }
      const num = Number(trimmedVal);
      if (trimmedVal === "" || Number.isNaN(num) || !Number.isFinite(num)) {
        showError(
          "INVALID_VARIABLE_VALUE",
          `Variable '${trimmedName}' must have a valid numeric value.`,
          null,
          expression
        );
        return;
      }
      varMap[trimmedName] = num;
    }

    setViewState("loading");

    try {
      const response = await fetch(buildUrl("/api/evaluate"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ expression, variables: varMap }),
      });

      const payload = await response.json().catch(() => null);

      if (!response.ok) {
        const err = payload?.error;
        showError(
          err?.code || `HTTP_${response.status}`,
          err?.message || `Server returned status ${response.status}`,
          err?.position ?? null,
          expression
        );
        return;
      }

      if (!payload || typeof payload.result === "undefined") {
        showError("INVALID_RESPONSE", "Received an unexpected response format from the API.", null, expression);
        return;
      }

      showSuccess(payload);
    } catch (err) {
      showError(
        "NETWORK_ERROR",
        `Could not reach the backend API (${buildUrl("/api/evaluate")}). Ensure the Spring Boot server is running.`,
        null,
        expression
      );
    }
  }

  // ── Demo Presets & Events ──────────────────────────────────────
  function loadPreset(expr, vars) {
    expressionInput.value = expr;
    variables = vars.map((v) => ({ ...v }));
    renderVariables();
    handleEvaluate();
  }

  btnDemo1.addEventListener("click", () => {
    loadPreset("-(a + b) * 2 + 30 / 5", [
      { name: "a", value: "10" },
      { name: "b", value: "5" },
    ]);
  });

  btnDemo2.addEventListener("click", () => {
    loadPreset("-((a + b) * (c - 5)) + 18 / (b - 1)", [
      { name: "a", value: "-4" },
      { name: "b", value: "3" },
      { name: "c", value: "2" },
    ]);
  });

  btnDemoErr.addEventListener("click", () => {
    loadPreset("a + (b * 2", [
      { name: "a", value: "1" },
      { name: "b", value: "2" },
    ]);
  });

  addVarBtn.addEventListener("click", () => {
    variables.push({ name: "", value: "" });
    renderVariables();
  });

  clearBtn.addEventListener("click", () => {
    expressionInput.value = "";
    variables = [];
    renderVariables();
    setViewState("initial");
    expressionInput.focus();
  });

  evaluateForm.addEventListener("submit", handleEvaluate);

  expressionInput.addEventListener("keydown", (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      handleEvaluate();
    }
  });

  tabButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const target = btn.dataset.tab;
      tabButtons.forEach((b) => {
        const active = b === btn;
        b.classList.toggle("active", active);
        b.setAttribute("aria-selected", String(active));
      });
      Object.entries(tabPanels).forEach(([key, panel]) => {
        panel.classList.toggle("hidden", key !== target);
        panel.classList.toggle("active", key === target);
      });
    });
  });

  toggleSettingsBtn.addEventListener("click", () => {
    const isHidden = settingsPanel.classList.toggle("hidden");
    toggleSettingsBtn.setAttribute("aria-expanded", String(!isHidden));
  });

  saveApiBtn.addEventListener("click", () => {
    apiBaseUrl = apiBaseInput.value.trim();
    localStorage.setItem("observatory_api_base", apiBaseUrl);
    checkHealth();
  });

  // ── Ambient Observatory Canvas (respects reduced motion) ──────
  function initCanvas() {
    const canvas = document.getElementById("observatory-canvas");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    const stars = Array.from({ length: 65 }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      r: Math.random() * 1.4 + 0.4,
      a: Math.random() * 0.55 + 0.15,
      phase: Math.random() * Math.PI * 2,
    }));

    let angle = 0;

    function draw() {
      ctx.clearRect(0, 0, width, height);

      // Stars
      for (const s of stars) {
        const twinkle = reducedMotion ? s.a : s.a + Math.sin(angle * 2 + s.phase) * 0.12;
        ctx.fillStyle = `rgba(226, 232, 240, ${Math.max(0.08, twinkle)})`;
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
        ctx.fill();
      }

      // Subtle orbiting observatory ring in top-right corner
      const cx = width - 110;
      const cy = 110;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.strokeStyle = "rgba(56, 189, 248, 0.08)";
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(0, 0, 72, 0, Math.PI * 2);
      ctx.stroke();

      ctx.rotate(angle);
      ctx.fillStyle = "rgba(243, 213, 155, 0.35)";
      ctx.beginPath();
      ctx.arc(72, 0, 2.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      if (!reducedMotion) {
        angle += 0.0025;
        requestAnimationFrame(draw);
      }
    }

    window.addEventListener("resize", () => {
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
      if (reducedMotion) draw();
    });

    draw();
  }

  // ── Initialize ─────────────────────────────────────────────────
  apiBaseInput.value = apiBaseUrl;
  expressionInput.value = "-(a + b) * 2 + 30 / 5";
  renderVariables();
  checkHealth();
  initCanvas();
})();
