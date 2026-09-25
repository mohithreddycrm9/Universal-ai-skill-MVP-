#!/usr/bin/env node
/**
 * Demo: get_build_suggestions output for Cursor-style chips.
 * Usage: node --experimental-sqlite scripts/demo-build-suggestions.mjs
 */
import { createGateway } from "../dist/gateway.js";
import { loadConfig } from "../dist/policy/load.js";
import { LocalSource } from "../dist/discovery/local-source.js";
function benignPackage() {
  return {
    sourceId: "local",
    publisher: "fixture-org",
    ownerLogin: "fixture-org",
    repository: "fixture-org/csv-normalize",
    repositoryUrl: "https://example.local/fixture-org/csv-normalize",
    commitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    version: "1.0.0",
    license: "MIT",
    archived: false,
    files: [
      {
        path: "SKILL.md",
        content: `---
name: csv-normalize
description: Normalize CSV headers without executing code
---

Use RFC4180-aware parsing for upload pipelines.
`,
      },
      { path: "LICENSE", content: "MIT License" },
    ],
  };
}

function chipBar(suggestions) {
  const chips = suggestions.map((s) => `[ ${s.label} ]`).join("  ");
  const width = 72;
  const line = chips.length > width ? `${chips.slice(0, width - 1)}…` : chips;
  return [
    "┌─ Build hints (above Agent input) " + "─".repeat(Math.max(0, width - 35)) + "┐",
    `│ ${line.padEnd(width)} │`,
    "├" + "─".repeat(width + 2) + "┤",
    "│ Plan, @ for context, / for commands".padEnd(width + 2) + " │",
    "└" + "─".repeat(width + 2) + "┘",
  ].join("\n");
}

function printScenario(title, data) {
  console.log("\n" + "=".repeat(72));
  console.log(title);
  console.log("=".repeat(72));
  const suggestions = data.suggestions ?? [];
  if (suggestions.length === 0) {
    console.log("\n(no chips —", data.emptyReason ?? "empty", ")\n");
    return;
  }
  console.log("\n" + chipBar(suggestions) + "\n");
  for (const s of suggestions) {
    console.log(`  • ${s.label}`);
    console.log(`    because: ${s.because}  |  action: ${s.action}  |  confidence: ${s.confidence}`);
    if (s.prompt) {
      console.log(`    prompt → ${s.prompt.slice(0, 120)}${s.prompt.length > 120 ? "…" : ""}`);
    }
    if (s.toolName) {
      console.log(`    tool → ${s.toolName}(${JSON.stringify(s.toolArgs ?? {})})`);
    }
  }
  if (data.buildContextUsed) {
    console.log("\n  buildContextUsed:", JSON.stringify(data.buildContextUsed, null, 2).replace(/\n/g, "\n  "));
  }
}

const config = loadConfig("config");
config.trust.verifiedPublishers = ["fixture-org"];
const local = new LocalSource();
local.register(benignPackage());
const gw = createGateway({ config, sources: [local] });

const empty = await gw.getBuildSuggestions({ requestId: "demo-1" });
printScenario("1) No build context (what NOT to do)", empty);

const realistic = await gw.getBuildSuggestions({
  requestId: "demo-2",
  agentState: "running",
  goal: "add CSV normalization API for uploaded files",
  activeStep: "wire handler and fix failing tests",
  changedFiles: ["src/gateway.ts", "tests/gateway.test.ts"],
  lastCommand: "npm test -- gateway",
  lastCommandExitCode: 1,
  recentEvents: [
    { kind: "test_failed", summary: "gateway.test.ts", files: ["tests/gateway.test.ts"] },
    { kind: "shell_pending", summary: "npm test -- gateway" },
  ],
  limit: 6,
  includeSkillHints: false,
});

printScenario("2) Live build: failing tests + shell approval (typical Cursor run)", realistic);

const skillGap = await gw.getBuildSuggestions({
  requestId: "demo-3",
  agentState: "running",
  goal: "normalize CSV headers in the upload pipeline",
  activeStep: "load trusted skill instructions",
  changedFiles: ["src/import/csv.ts"],
  recentEvents: [{ kind: "skill_gap", summary: "need csv-normalize skill" }],
  limit: 5,
  includeSkillHints: true,
});

printScenario("3) Same build + skill_gap (may add matched local skill chip)", skillGap);
