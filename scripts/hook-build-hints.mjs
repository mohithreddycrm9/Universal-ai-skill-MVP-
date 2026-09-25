#!/usr/bin/env node
/**
 * Cursor postToolUse hook: inject ## Build hints into agent context.
 * stdin: Cursor hook JSON; stdout: { "additional_context": "..." } or {}
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { computeBuildSuggestions } from "../dist/agent/build-suggestions.js";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const stateDir = join(root, ".cursor/hooks/state");
const statePath = join(stateDir, "build.json");

function readStdin() {
  return readFileSync(0, "utf8");
}

function loadState() {
  if (!existsSync(statePath)) {
    return {
      changedFiles: [],
      recentEvents: [],
      lastCommand: undefined,
      lastCommandExitCode: undefined,
    };
  }
  try {
    return JSON.parse(readFileSync(statePath, "utf8"));
  } catch {
    return { changedFiles: [], recentEvents: [], lastCommand: undefined, lastCommandExitCode: undefined };
  }
}

function saveState(state) {
  mkdirSync(stateDir, { recursive: true });
  writeFileSync(statePath, JSON.stringify(state, null, 2));
}

function loadGoal() {
  const path = join(root, ".cursor/build-goal.txt");
  if (!existsSync(path)) {
    return undefined;
  }
  const text = readFileSync(path, "utf8").trim();
  return text || undefined;
}

function pushEvent(state, event) {
  state.recentEvents = [...(state.recentEvents ?? []), event].slice(-24);
}

function parseShellOutput(toolOutput) {
  if (!toolOutput) {
    return {};
  }
  try {
    const parsed = JSON.parse(toolOutput);
    const exit =
      parsed.exitCode ?? parsed.exit_code ?? parsed.code ?? parsed.status;
    return { exitCode: typeof exit === "number" ? exit : undefined, raw: parsed };
  } catch {
    const failed = /error|failed|exit code [1-9]/i.test(toolOutput);
    return { exitCode: failed ? 1 : 0 };
  }
}

function applyTool(state, payload) {
  const name = payload.tool_name ?? "";
  let input = payload.tool_input;
  if (typeof input === "string") {
    try {
      input = JSON.parse(input);
    } catch {
      input = {};
    }
  }

  if (name === "Write" || name === "StrReplace" || name === "EditNotebook") {
    const path =
      input?.path ?? input?.file_path ?? input?.target_file ?? input?.notebook_path;
    if (path && typeof path === "string") {
      const rel = path.startsWith(root) ? path.slice(root.length + 1) : path;
      state.changedFiles = [...new Set([...(state.changedFiles ?? []), rel])].slice(-24);
    }
    return;
  }

  if (name === "Shell") {
    const command = input?.command ?? input?.cmd;
    if (typeof command === "string") {
      state.lastCommand = command;
    }
    const { exitCode } = parseShellOutput(payload.tool_output);
    if (exitCode !== undefined) {
      state.lastCommandExitCode = exitCode;
      if (exitCode !== 0) {
        if (/test|vitest|jest|pytest/i.test(state.lastCommand ?? "")) {
          pushEvent(state, { kind: "test_failed", summary: state.lastCommand });
        } else {
          pushEvent(state, { kind: "build_failed", summary: state.lastCommand });
        }
      }
    }
    if (/npm test|vitest|jest run|pytest/i.test(command ?? "")) {
      pushEvent(state, { kind: "shell_pending", summary: command });
    }
  }
}

function formatContext(result) {
  if (!result.suggestions?.length) {
    return null;
  }
  const lines = [
    "## Build hints (for the user — show this section in your reply)",
    "",
    "| Chip | Why |",
    "|------|-----|",
  ];
  for (const s of result.suggestions) {
    lines.push(`| ${s.label.replace(/\|/g, "\\|")} | ${s.because ?? "—"} |`);
  }
  lines.push("", "**Prompts (user can copy into composer):**");
  result.suggestions.forEach((s, i) => {
    lines.push(`${i + 1}. **${s.label}**`);
    if (s.prompt) {
      lines.push(`   ${s.prompt}`);
    }
  });
  lines.push("", "_Do not skip this block. User must see build hints in chat._");
  return lines.join("\n");
}

async function main() {
  let payload;
  try {
    payload = JSON.parse(readStdin());
  } catch {
    process.stdout.write("{}\n");
    return;
  }

  const state = loadState();
  applyTool(state, payload);
  saveState(state);

  const goal = loadGoal();
  const result = computeBuildSuggestions({
    agentState: "running",
    goal,
    activeStep: "Cursor agent build in progress",
    changedFiles: state.changedFiles,
    lastCommand: state.lastCommand,
    lastCommandExitCode: state.lastCommandExitCode,
    recentEvents: state.recentEvents,
    limit: 5,
  });

  const additional_context = formatContext(result);
  if (!additional_context) {
    process.stdout.write("{}\n");
    return;
  }
  process.stdout.write(JSON.stringify({ additional_context }) + "\n");
}

main().catch(() => process.stdout.write("{}\n"));
