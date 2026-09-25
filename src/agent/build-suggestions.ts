import { newId } from "../util/ids.js";
import { iso, systemClock, type Clock } from "../util/clock.js";

export type AgentRunState = "running" | "waiting_for_user" | "idle" | "failed";

export type SuggestionAction = "insert_prompt" | "invoke_tool" | "cursor_setting" | "note";

export type SuggestionSource = "heuristic" | "gateway" | "discovery";
export type SuggestionConfidence = "high" | "medium" | "low";

export interface AgentEventHint {
  /** Machine-friendly kind tied to the current build, e.g. test_failed, shell_pending */
  kind: string;
  summary?: string;
  files?: string[];
  serverName?: string;
}

export interface BuildSuggestionsInput {
  agentState?: AgentRunState;
  /** User's stated build goal — required for most suggestions */
  goal?: string;
  /** What the agent is doing right now in service of the goal */
  activeStep?: string;
  changedFiles?: string[];
  lastCommand?: string;
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
  relevance: "high" | "medium";
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
  cursorSetting?: string;
  /** Why this chip is shown (build signal) */
  because?: string;
}

export interface BuildSuggestionsResult {
  agentState: AgentRunState;
  suggestions: BuildSuggestion[];
  generatedAt: string;
  buildContextUsed?: {
    goal?: string;
    activeStep?: string;
    changedFiles: string[];
    eventKinds: string[];
  };
  emptyReason?: string;
}

export interface GatewayEnrichment {
  pendingCostApprovalCount: number;
  pendingCapabilityApprovalCount: number;
  localSkillHints: SkillHint[];
  discoveryHints: SkillHint[];
}

const MAX_EVENTS = 24;
const GOAL_SNIPPET = 200;

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

function eventsOfKind(events: AgentEventHint[], kind: string): AgentEventHint[] {
  return events.filter((event) => event.kind === kind);
}

function eventSummary(events: AgentEventHint[], kind: string): string | undefined {
  return events.find((event) => event.kind === kind)?.summary;
}

function goalPhrase(goal?: string): string | undefined {
  const trimmed = goal?.trim();
  if (!trimmed) {
    return undefined;
  }
  return trimmed.length > GOAL_SNIPPET ? `${trimmed.slice(0, GOAL_SNIPPET)}…` : trimmed;
}

function uniqueFiles(input: BuildSuggestionsInput, events: AgentEventHint[]): string[] {
  const fromInput = input.changedFiles ?? [];
  const fromEvents = events.flatMap((event) => event.files ?? []);
  return [...new Set([...fromInput, ...fromEvents].map((f) => f.trim()).filter(Boolean))].slice(0, 12);
}

function hasBuildSignals(input: BuildSuggestionsInput, events: AgentEventHint[]): boolean {
  if (goalPhrase(input.goal)) {
    return true;
  }
  if (input.activeStep?.trim()) {
    return true;
  }
  if (events.length > 0) {
    return true;
  }
  if ((input.changedFiles?.length ?? 0) > 0) {
    return true;
  }
  if (input.lastCommand?.trim()) {
    return true;
  }
  if (input.lastCommandExitCode !== undefined) {
    return true;
  }
  if ((input.filesChangedCount ?? 0) > 0) {
    return true;
  }
  return false;
}

export function goalTokens(goal: string): string[] {
  return goal
    .toLowerCase()
    .split(/[^a-z0-9]+/g)
    .filter((token) => token.length >= 3)
    .slice(0, 12);
}

export function skillMatchesGoal(goal: string, name: string, description?: string): boolean {
  const tokens = goalTokens(goal);
  if (tokens.length === 0) {
    return false;
  }
  const hay = `${name} ${description ?? ""}`.toLowerCase();
  return tokens.some((token) => hay.includes(token));
}

export function buildBlockedOnApprovals(events: AgentEventHint[]): boolean {
  return (
    hasEvent(events, "cost_approval_pending") ||
    hasEvent(events, "capability_approval_pending") ||
    hasEvent(events, "approval_required") ||
    hasEvent(events, "acquire_skill") ||
    hasEvent(events, "scan_skill")
  );
}

export function hasSkillGapEvent(events: AgentEventHint[]): boolean {
  return (
    hasEvent(events, "skill_gap") ||
    hasEvent(events, "acquire_job_failed") ||
    hasEvent(events, "skill_verify_failed")
  );
}

export function shouldOfferSkillHints(goal: string | undefined, events: AgentEventHint[]): boolean {
  if (!goalPhrase(goal)) {
    return hasEvent(events, "skill_gap") || hasEvent(events, "acquire_job_failed");
  }
  return (
    hasEvent(events, "skill_gap") ||
    hasEvent(events, "acquire_job_failed") ||
    hasEvent(events, "skill_verify_failed")
  );
}

/**
 * Build-scoped hints only. Callers must pass goal, activeStep, changedFiles, and recentEvents
 * from the live agent run; otherwise suggestions stay empty.
 */
export function computeBuildSuggestions(
  input: BuildSuggestionsInput,
  clock: Clock = systemClock,
): BuildSuggestionsResult {
  const agentState = input.agentState ?? "running";
  const events = (input.recentEvents ?? []).slice(-MAX_EVENTS);
  const limit = clampLimit(input.limit);
  const goal = goalPhrase(input.goal);
  const step = input.activeStep?.trim();
  const changedFiles = uniqueFiles(input, events);
  const raw: BuildSuggestion[] = [];

  if (!hasBuildSignals(input, events)) {
    return {
      agentState,
      suggestions: [],
      generatedAt: iso(clock),
      emptyReason:
        "Pass goal, activeStep, changedFiles, and/or recentEvents from the current build so suggestions stay on-task.",
    };
  }

  const buildContextUsed = {
    goal,
    activeStep: step,
    changedFiles,
    eventKinds: [...new Set(events.map((event) => event.kind))],
  };

  if (hasEvent(events, "shell_pending") || hasEvent(events, "terminal_approval")) {
    const cmd = input.lastCommand?.trim() || eventSummary(events, "shell_pending");
    add(raw, {
      label: cmd ? `Approve command for this build: ${truncateLabel(cmd)}` : "Approve shell step for this build",
      prompt: goal
        ? `Before approving the shell command, explain how it advances "${goal}"${cmd ? ` (command: ${cmd})` : ""}.`
        : cmd
          ? `Before approving, explain why this command is needed: ${cmd}`
          : "Before approving, explain why this shell command is needed for the current build step.",
      action: "insert_prompt",
      priority: 95,
      source: "heuristic",
      confidence: "high",
      because: "shell_pending",
    });
  }

  if (hasEvent(events, "mcp_auth_error")) {
    const server = events.find((event) => event.kind === "mcp_auth_error")?.serverName ?? "MCP server";
    add(raw, {
      label: `Unblock build: authenticate ${server}`,
      prompt: goal
        ? `MCP auth failed for ${server} while working on "${goal}". Walk me through auth without weakening security.`
        : `MCP auth failed for ${server}. Walk me through auth without weakening security.`,
      action: "insert_prompt",
      priority: 98,
      source: "heuristic",
      confidence: "high",
      because: "mcp_auth_error",
    });
  }

  const exitCode = input.lastCommandExitCode;
  const lastCommand = input.lastCommand?.trim();
  if (exitCode !== undefined && exitCode !== 0 && (lastCommand || goal)) {
    add(raw, {
      label: lastCommand ? `Fix failed: ${truncateLabel(lastCommand)}` : "Fix failed build command",
      prompt: goal
        ? `Command failed (exit ${exitCode})${lastCommand ? `: \`${lastCommand}\`` : ""}. Fix the smallest change needed for "${goal}" and re-run the same command.`
        : `Command failed (exit ${exitCode})${lastCommand ? `: \`${lastCommand}\`` : ""}. Diagnose from output and apply a minimal fix, then re-run.`,
      action: "insert_prompt",
      priority: 92,
      source: "heuristic",
      confidence: "high",
      because: "command_failed",
    });
  }

  for (const failed of eventsOfKind(events, "test_failed")) {
    const target = failed.summary ?? failed.files?.[0];
    add(raw, {
      label: target ? `Fix tests: ${truncateLabel(target)}` : "Fix failing tests in this build",
      prompt: goal
        ? `Tests failed${target ? ` (${target})` : ""} while building "${goal}". Fix with minimal diff and re-run the same test command.`
        : `Tests failed${target ? ` (${target})` : ""}. Fix with minimal diff and re-run the same test command.`,
      action: "insert_prompt",
      priority: 94,
      source: "heuristic",
      confidence: "high",
      because: "test_failed",
    });
  }

  for (const kind of ["lint_failed", "typecheck_failed", "build_failed"] as const) {
    if (!hasEvent(events, kind)) {
      continue;
    }
    const summary = eventSummary(events, kind);
    add(raw, {
      label: summary ? `Resolve ${kind.replace("_", " ")}: ${truncateLabel(summary)}` : `Resolve ${kind.replace("_", " ")}`,
      prompt: goal
        ? `${kind.replace("_", " ")}${summary ? ` (${summary})` : ""} blocks "${goal}". Fix only what this build touched.`
        : `${kind.replace("_", " ")}${summary ? ` (${summary})` : ""}. Fix with minimal changes.`,
      action: "insert_prompt",
      priority: 93,
      source: "heuristic",
      confidence: "high",
      because: kind,
    });
  }

  const filesChanged = input.filesChangedCount ?? changedFiles.length;
  if ((filesChanged > 3 || hasEvent(events, "multi_file_diff")) && changedFiles.length > 0) {
    const sample = changedFiles.slice(0, 3).join(", ");
    add(raw, {
      label: `Review build diff (${filesChanged} files)`,
      prompt: goal
        ? `Summarize changes in ${sample}${filesChanged > 3 ? ", …" : ""} for "${goal}" and pause until I confirm before more edits.`
        : `Summarize changes in ${sample}${filesChanged > 3 ? ", …" : ""} and pause until I confirm.`,
      action: "insert_prompt",
      priority: 86,
      source: "heuristic",
      confidence: "high",
      because: "multi_file_diff",
    });
  } else if (changedFiles.length === 1 || changedFiles.length === 2) {
    for (const file of changedFiles) {
      add(raw, {
        label: `Verify ${truncateLabel(file)}`,
        prompt: goal
          ? `For "${goal}", run the smallest check that validates \`${file}\` (tests or typecheck) and report results.`
          : `Run the smallest check that validates \`${file}\` and report results.`,
        action: "insert_prompt",
        priority: 78,
        source: "heuristic",
        confidence: "medium",
        because: "file_changed",
      });
    }
  }

  if (hasEvent(events, "acquire_job_failed") || hasEvent(events, "skill_verify_failed")) {
    const summary = eventSummary(events, "acquire_job_failed") ?? eventSummary(events, "skill_verify_failed");
    add(raw, {
      label: "Check skill step blocking this build",
      prompt: goal
        ? `Skill step failed${summary ? ` (${summary})` : ""} for "${goal}". Call get_skill_status and explain the blocker.`
        : `Skill step failed${summary ? ` (${summary})` : ""}. Call get_skill_status and explain the blocker.`,
      action: "invoke_tool",
      priority: 88,
      source: "heuristic",
      confidence: "high",
      toolName: "get_skill_status",
      toolArgs: {},
      because: "skill_lifecycle",
    });
  }

  if (agentState === "waiting_for_user" && step && goal) {
    add(raw, {
      label: `Confirm step: ${truncateLabel(step)}`,
      prompt: `For "${goal}", confirm before continuing: ${step}`,
      action: "insert_prompt",
      priority: 82,
      source: "heuristic",
      confidence: "high",
      because: "waiting_on_step",
    });
  } else if (agentState === "waiting_for_user" && goal) {
    const question = eventSummary(events, "agent_question");
    add(raw, {
      label: question ? `Answer: ${truncateLabel(question)}` : `Answer to continue "${truncateLabel(goal)}"`,
      prompt: question
        ? `My answer for "${goal}" (${question}): `
        : `My answer so you can continue "${goal}": `,
      action: "insert_prompt",
      priority: 80,
      source: "heuristic",
      confidence: "medium",
      because: "waiting_for_user",
    });
  }

  if (agentState === "failed" && goal) {
    add(raw, {
      label: `Retry build: ${truncateLabel(goal)}`,
      prompt: `Retry "${goal}" with a minimal diff. State assumptions and ask before destructive steps.`,
      action: "insert_prompt",
      priority: 96,
      source: "heuristic",
      confidence: "high",
      because: "build_failed_state",
    });
  }

  if (step && goal && agentState === "running" && raw.length === 0) {
    add(raw, {
      label: `Next for: ${truncateLabel(step)}`,
      prompt: `Continue "${goal}" — current step: ${step}. List what you will change before editing.`,
      action: "insert_prompt",
      priority: 70,
      source: "heuristic",
      confidence: "medium",
      because: "active_step",
    });
  }

  const suggestions = dedupeAndRank(raw).slice(0, limit);

  return {
    agentState,
    suggestions,
    generatedAt: iso(clock),
    buildContextUsed,
    emptyReason: suggestions.length === 0 ? "No suggestions matched the build signals provided." : undefined,
  };
}

export function mergeGatewayEnrichment(
  base: BuildSuggestionsResult,
  enrichment: GatewayEnrichment,
  limit: number,
  context: { goal?: string; recentEvents?: AgentEventHint[]; includeSkillHints: boolean },
): BuildSuggestionsResult {
  const extra: BuildSuggestion[] = [...base.suggestions];
  const events = context.recentEvents ?? [];
  const goal = goalPhrase(context.goal);

  const pendingTotal =
    enrichment.pendingCostApprovalCount + enrichment.pendingCapabilityApprovalCount;
  if (pendingTotal > 0 && buildBlockedOnApprovals(events)) {
    add(extra, {
      label: `Approval blocking this build (${pendingTotal})`,
      prompt: goal
        ? `List pending approvals blocking "${goal}" and what each unlocks. I will approve via CLI after review.`
        : "List pending approvals for this build and what each unlocks.",
      action: "invoke_tool",
      priority: 97,
      source: "gateway",
      confidence: "high",
      toolName: "list_pending_cost_approvals",
      toolArgs: {},
      because: "approval_pending",
    });
  }

  if (context.includeSkillHints && goal && shouldOfferSkillHints(goal, events)) {
    for (const hint of enrichment.localSkillHints) {
      add(extra, {
        id: hint.skillId ? `local-${hint.skillId}` : undefined,
        label: hint.label,
        prompt: hint.prompt,
        action: hint.skillId ? "invoke_tool" : "insert_prompt",
        priority: hint.relevance === "high" ? 76 : 70,
        source: "gateway",
        confidence: hint.relevance === "high" ? "high" : "medium",
        toolName: hint.skillId ? "get_skill" : undefined,
        toolArgs: hint.skillId ? { skillId: hint.skillId, level: 1 } : undefined,
        because: "skill_match",
      });
    }

    for (const hint of enrichment.discoveryHints) {
      add(extra, {
        label: hint.label,
        prompt: hint.prompt,
        action: "invoke_tool",
        priority: 68,
        source: "discovery",
        confidence: "medium",
        toolName: "acquire_skill",
        toolArgs: { query: hint.name, wait: false },
        because: "skill_discovery",
      });
    }
  }

  const suggestions = dedupeAndRank(extra).slice(0, clampLimit(limit));

  return {
    ...base,
    suggestions,
    emptyReason: suggestions.length === 0 ? base.emptyReason ?? "No build-scoped suggestions." : undefined,
  };
}

function truncateLabel(text: string, max = 48): string {
  const oneLine = text.replace(/\s+/g, " ").trim();
  return oneLine.length <= max ? oneLine : `${oneLine.slice(0, max - 1)}…`;
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
