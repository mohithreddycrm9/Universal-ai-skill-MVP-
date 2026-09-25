import { newId } from "../util/ids.js";
import { iso, systemClock, type Clock } from "../util/clock.js";

export type AgentRunState = "running" | "waiting_for_user" | "idle" | "failed";

export type SuggestionAction =
  | "insert_prompt"
  | "invoke_tool"
  | "cursor_setting"
  | "note";

export type SuggestionSource = "heuristic" | "gateway" | "discovery";
export type SuggestionConfidence = "high" | "medium" | "low";

export interface AgentEventHint {
  /** Machine-friendly kind, e.g. shell_pending, test_failed, mcp_auth_error */
  kind: string;
  summary?: string;
  files?: string[];
  serverName?: string;
}

export interface BuildSuggestionsInput {
  agentState?: AgentRunState;
  goal?: string;
  recentEvents?: AgentEventHint[];
  filesChangedCount?: number;
  lastCommandExitCode?: number;
  limit?: number;
}

export interface SkillHint {
  skillId?: string;
  name: string;
  label: string;
  prompt: string;
}

export interface BuildSuggestion {
  id: string;
  label: string;
  prompt?: string;
  action: SuggestionAction;
  priority: number;
  source: SuggestionSource;
  confidence: SuggestionConfidence;
  toolName?: string;
  toolArgs?: Record<string, unknown>;
  /** Cursor settings path or deep-link hint for the client */
  cursorSetting?: string;
}

export interface BuildSuggestionsResult {
  agentState: AgentRunState;
  suggestions: BuildSuggestion[];
  generatedAt: string;
}

export interface GatewayEnrichment {
  pendingCostApprovalCount: number;
  pendingCapabilityApprovalCount: number;
  localSkillHints: SkillHint[];
  discoveryHints: SkillHint[];
}

const MAX_EVENTS = 24;

function clampLimit(limit?: number): number {
  return Math.min(Math.max(limit ?? 5, 1), 12);
}

function add(
  list: BuildSuggestion[],
  partial: Omit<BuildSuggestion, "id"> & { id?: string },
): void {
  list.push({ ...partial, id: partial.id ?? newId("sug") });
}

function hasEvent(events: AgentEventHint[], kind: string): boolean {
  return events.some((event) => event.kind === kind);
}

function eventSummary(events: AgentEventHint[], kind: string): string | undefined {
  return events.find((event) => event.kind === kind)?.summary;
}

/**
 * Deterministic, host-agnostic hints for proactive agent UI (e.g. chips above Cursor Agent input).
 * Does not call external APIs; gateway may attach skill / approval hints separately.
 */
export function computeBuildSuggestions(
  input: BuildSuggestionsInput,
  clock: Clock = systemClock,
): BuildSuggestionsResult {
  const agentState = input.agentState ?? "running";
  const events = (input.recentEvents ?? []).slice(-MAX_EVENTS);
  const limit = clampLimit(input.limit);
  const raw: BuildSuggestion[] = [];

  if (agentState === "running") {
    add(raw, {
      label: "Queue a follow-up for after this turn",
      prompt: "After you finish this step, also: ",
      action: "insert_prompt",
      priority: 40,
      source: "heuristic",
      confidence: "medium",
    });
    add(raw, {
      label: "Steer now at next tool boundary (Cursor: Send now)",
      prompt: "",
      action: "note",
      priority: 35,
      source: "heuristic",
      confidence: "high",
      cursorSetting: "agents.conversation.newMessages",
    });
  }

  if (hasEvent(events, "shell_pending") || hasEvent(events, "terminal_approval")) {
    add(raw, {
      label: "Review terminal command before approving",
      prompt: "Pause before running shell commands. Show me the exact command and why it is needed.",
      action: "insert_prompt",
      priority: 95,
      source: "heuristic",
      confidence: "high",
    });
  }

  if (hasEvent(events, "mcp_auth_error")) {
    const server = events.find((e) => e.kind === "mcp_auth_error")?.serverName ?? "MCP server";
    add(raw, {
      label: `Authenticate ${server}`,
      prompt: `MCP auth failed for ${server}. Guide me to authenticate without disabling security checks.`,
      action: "insert_prompt",
      priority: 98,
      source: "heuristic",
      confidence: "high",
    });
  }

  const exitCode = input.lastCommandExitCode;
  if (exitCode !== undefined && exitCode !== 0) {
    add(raw, {
      label: "Fix failing command output",
      prompt: "The last command failed. Diagnose from the stderr/stdout above and apply a minimal fix.",
      action: "insert_prompt",
      priority: 90,
      source: "heuristic",
      confidence: "high",
    });
  }

  if (hasEvent(events, "test_failed")) {
    const summary = eventSummary(events, "test_failed");
    add(raw, {
      label: "Fix failing tests",
      prompt: summary
        ? `Tests failed (${summary}). Fix with the smallest diff and re-run the same test command.`
        : "Tests failed. Fix with the smallest diff and re-run the same test command.",
      action: "insert_prompt",
      priority: 92,
      source: "heuristic",
      confidence: "high",
    });
  }

  const filesChanged = input.filesChangedCount ?? 0;
  if (filesChanged > 3 || hasEvent(events, "multi_file_diff")) {
    add(raw, {
      label: "Review multi-file diff before continuing",
      prompt: "Summarize what changed in each file and wait for my OK before more edits.",
      action: "insert_prompt",
      priority: 85,
      source: "heuristic",
      confidence: "high",
    });
  }

  if (hasEvent(events, "acquire_job_failed") || hasEvent(events, "skill_verify_failed")) {
    add(raw, {
      label: "Check skill acquisition status",
      prompt: "Use get_skill_status for the skill/job id and explain the lifecycle blocker.",
      action: "invoke_tool",
      priority: 88,
      source: "heuristic",
      confidence: "high",
      toolName: "get_skill_status",
      toolArgs: {},
    });
  }

  if (input.goal?.trim()) {
    add(raw, {
      label: "Discover a verified skill for this task",
      prompt: "",
      action: "invoke_tool",
      priority: 70,
      source: "heuristic",
      confidence: "medium",
      toolName: "discover_skill",
      toolArgs: { query: input.goal.trim().slice(0, 200), limit: 5 },
    });
    add(raw, {
      label: "Keep scope minimal",
      prompt: `Stay focused on: ${input.goal.trim().slice(0, 240)}. Do not refactor unrelated files.`,
      action: "insert_prompt",
      priority: 65,
      source: "heuristic",
      confidence: "medium",
    });
  }

  if (agentState === "waiting_for_user") {
    add(raw, {
      label: "Reply with constraints (env, risk, deadline)",
      prompt: "My constraints: ",
      action: "insert_prompt",
      priority: 80,
      source: "heuristic",
      confidence: "medium",
    });
  }

  if (agentState === "failed") {
    add(raw, {
      label: "Retry with smaller scope",
      prompt: "Retry with a minimal diff. List assumptions and ask before destructive steps.",
      action: "insert_prompt",
      priority: 96,
      source: "heuristic",
      confidence: "high",
    });
  }

  if (agentState === "idle") {
    add(raw, {
      label: "Start agent with a clear goal",
      prompt: "Goal: ",
      action: "insert_prompt",
      priority: 50,
      source: "heuristic",
      confidence: "low",
    });
  }

  return {
    agentState,
    suggestions: dedupeAndRank(raw).slice(0, limit),
    generatedAt: iso(clock),
  };
}

export function mergeGatewayEnrichment(
  base: BuildSuggestionsResult,
  enrichment: GatewayEnrichment,
  limit: number,
): BuildSuggestionsResult {
  const extra: BuildSuggestion[] = [...base.suggestions];

  const pendingTotal =
    enrichment.pendingCostApprovalCount + enrichment.pendingCapabilityApprovalCount;
  if (pendingTotal > 0) {
    add(extra, {
      label: `Review ${pendingTotal} pending approval(s)`,
      prompt: "",
      action: "invoke_tool",
      priority: 97,
      source: "gateway",
      confidence: "high",
      toolName: "list_pending_cost_approvals",
      toolArgs: {},
    });
    add(extra, {
      label: "Approve via CLI after review",
      prompt: "List pending approval ids. I will run skill-mcp approve <id> in a terminal after review.",
      action: "insert_prompt",
      priority: 75,
      source: "gateway",
      confidence: "medium",
    });
  }

  for (const hint of enrichment.localSkillHints) {
    add(extra, {
      id: hint.skillId ? `local-${hint.skillId}` : undefined,
      label: hint.label,
      prompt: hint.prompt,
      action: hint.skillId ? "invoke_tool" : "insert_prompt",
      priority: 72,
      source: "gateway",
      confidence: "medium",
      toolName: hint.skillId ? "get_skill" : undefined,
      toolArgs: hint.skillId ? { skillId: hint.skillId, level: 1 } : undefined,
    });
  }

  for (const hint of enrichment.discoveryHints) {
    add(extra, {
      label: hint.label,
      prompt: hint.prompt,
      action: "invoke_tool",
      priority: 68,
      source: "discovery",
      confidence: "low",
      toolName: "acquire_skill",
      toolArgs: { query: hint.name, wait: false },
    });
  }

  return {
    ...base,
    suggestions: dedupeAndRank(extra).slice(0, clampLimit(limit)),
  };
}

function dedupeAndRank(items: BuildSuggestion[]): BuildSuggestion[] {
  const seen = new Set<string>();
  const out: BuildSuggestion[] = [];
  const sorted = [...items].sort((a, b) => b.priority - a.priority);
  for (const item of sorted) {
    const key = `${item.action}:${item.label}:${item.toolName ?? ""}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    out.push(item);
  }
  return out;
}
