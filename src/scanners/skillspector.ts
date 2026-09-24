import { existsSync, mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { Finding, FindingSeverity, ScanTarget, ScannerRun, SecurityStatus } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";
import { CostDetector } from "../cost/detector.js";
import type { CostPolicy } from "../cost/types.js";
import { defaultSpawn, missingBinary, type SpawnFn } from "../util/spawn.js";

const DEFAULT_FREE_ONLY: CostPolicy = {
  policy: "ALLOW_FREE_ONLY",
  currency: "USD",
  allowUpToAmount: 0,
  preferFreeAlternatives: true,
  neverAutoPaidFallback: true,
  unknownCostRequiresApproval: true,
};

type SkillspectorReport = {
  risk_assessment?: {
    score?: number;
    severity?: string;
    recommendation?: string;
  };
  issues?: Array<{
    id?: string;
    severity?: string;
    finding?: string;
    explanation?: string;
    location?: { file?: string };
  }>;
  metadata?: { skillspector_version?: string };
};

/**
 * Adapter for NVIDIA SkillSpector (https://github.com/NVIDIA/SkillSpector, Apache-2.0).
 * Invokes: `skillspector scan <path> --format json` (+ `--no-llm` by default).
 * Missing binary → ERROR, never PASS.
 */
export class SkillspectorScanner implements SecurityScanner {
  readonly id = "skillspector";
  readonly version = "adapter-1.0.0-nvidia-skillspector";
  readonly cost = COST_CATALOG.skillspector;
  private readonly clock: Clock;
  private readonly spawn: SpawnFn;
  private readonly defaultCostPolicy: CostPolicy;

  constructor(clock?: Clock, spawn?: SpawnFn, costPolicy?: CostPolicy) {
    this.clock = clock ?? systemClock;
    this.spawn = spawn ?? defaultSpawn;
    this.defaultCostPolicy = costPolicy ?? DEFAULT_FREE_ONLY;
  }

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const binary = typeof configuration.binary === "string" ? configuration.binary : "skillspector";
    const failOpen = configuration.failOpen === true;
    const useLlm = configuration.useLlm === true;
    const scanRoot = materializeTarget(target);

    const found = this.spawn(binary, ["--version"], { timeout: 4000 });
    if (missingBinary(found) || found.error || found.status !== 0) {
      if (failOpen) {
        return runEnvelope(
          this.id,
          this.version,
          this.clock,
          "INCONCLUSIVE",
          [],
          "SkillSpector binary unavailable; failOpen is INCONCLUSIVE, never PASS. Install: pip install git+https://github.com/NVIDIA/skillspector.git",
        );
      }
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "ERROR",
        [],
        `SkillSpector not available (${binary}). Scan not performed. This is not PASS. See docs/SKILLSPECTOR.md`,
      );
    }
    const cliVersion = found.stdout.trim().split("\n")[0] || this.version;

    if (useLlm) {
      const policy =
        configuration.costPolicy && typeof configuration.costPolicy === "object"
          ? (configuration.costPolicy as CostPolicy)
          : this.defaultCostPolicy;
      const detector = new CostDetector(policy);
      const approvalStatus = configuration.costApprovalStatus;
      const approvalId =
        typeof configuration.costApprovalId === "string" ? configuration.costApprovalId : undefined;
      const decision = detector.evaluate(
        "scan_skill:skillspector_llm",
        COST_CATALOG.skillspector_llm,
        approvalStatus === "APPROVED" && approvalId
          ? { approvalStatus: "APPROVED", approvalId }
          : approvalStatus === "REJECTED"
            ? { approvalStatus: "REJECTED" }
            : {},
      );
      if (!decision.proceed) {
        const note =
          decision.kind === "NEEDS_APPROVAL"
            ? "SkillSpector LLM analysis requires explicit approval under current cost policy. No LLM request was made. Use useLlm:false for static-only scan. NOT_RUN ≠ PASS."
            : `SkillSpector LLM blocked by cost policy: ${decision.reason} NOT_RUN ≠ PASS.`;
        return runEnvelope(this.id, cliVersion, this.clock, "NOT_RUN", [], note);
      }
    }

    const args = ["scan", scanRoot, "--format", "json"];
    if (!useLlm) {
      args.push("--no-llm");
    }
    const baseline =
      typeof configuration.baselinePath === "string" && configuration.baselinePath.length > 0
        ? configuration.baselinePath
        : undefined;
    if (baseline) {
      args.push("--baseline", baseline);
    }

    const result = this.spawn(binary, args, {
      timeout: typeof configuration.timeoutMs === "number" ? configuration.timeoutMs : 180_000,
      cwd: scanRoot,
    });
    if (result.error?.code === "ETIMEDOUT") {
      return runEnvelope(this.id, cliVersion, this.clock, "TIMEOUT", [], "SkillSpector timed out. TIMEOUT ≠ PASS.");
    }

    const stdout = result.stdout?.trim() ?? "";
    if (!stdout.startsWith("{")) {
      return runEnvelope(
        this.id,
        cliVersion,
        this.clock,
        "INCONCLUSIVE",
        [],
        `SkillSpector produced no JSON report (exit ${result.status ?? "?"}). INCONCLUSIVE ≠ PASS.`,
      );
    }

    let report: SkillspectorReport;
    try {
      report = JSON.parse(stdout) as SkillspectorReport;
    } catch {
      return runEnvelope(
        this.id,
        cliVersion,
        this.clock,
        "ERROR",
        [],
        "SkillSpector JSON parse failed. This is not PASS.",
      );
    }

    const toolVersion = report.metadata?.skillspector_version ?? cliVersion;
    const findings = mapIssues(report.issues);
    const maxFindings =
      typeof configuration.maxFindings === "number" && configuration.maxFindings > 0
        ? configuration.maxFindings
        : 50;
    const clipped = findings.slice(0, maxFindings);
    const recommendation = String(report.risk_assessment?.recommendation ?? "").toUpperCase();
    const score = report.risk_assessment?.score;

    let status: SecurityStatus = "PASS";
    if (clipped.some((item) => item.severity === "HIGH" || item.severity === "CRITICAL")) {
      status = "FAIL";
    } else if (recommendation === "DO_NOT_INSTALL" || recommendation === "BLOCK") {
      status = "FAIL";
    } else if (clipped.length > 0) {
      status = "FAIL";
    } else if (result.status !== 0 && recommendation !== "SAFE") {
      status = "INCONCLUSIVE";
    }

    const notes = [
      `SkillSpector static=${!useLlm} score=${score ?? "n/a"} recommendation=${recommendation || "n/a"}.`,
      "Configured SkillSpector check only; not a universal safety claim.",
    ].join(" ");

    return runEnvelope(this.id, toolVersion, this.clock, status, clipped, notes);
  }
}

function materializeTarget(target: ScanTarget): string {
  if (target.quarantinePath && existsSync(target.quarantinePath)) {
    return target.quarantinePath;
  }
  const dir = mkdtempSync(join(tmpdir(), "skill-mcp-skillspector-"));
  mkdirSync(dir, { recursive: true });
  for (const file of target.package.files) {
    const safePath = file.path.replaceAll("..", "").replaceAll("/", "_");
    writeFileSync(join(dir, safePath), file.content, "utf8");
  }
  return dir;
}

function mapSeverity(raw: string | undefined): FindingSeverity {
  const value = String(raw ?? "MEDIUM").toUpperCase();
  if (value === "CRITICAL") return "CRITICAL";
  if (value === "HIGH") return "HIGH";
  if (value === "LOW") return "LOW";
  return "MEDIUM";
}

function mapIssues(issues: SkillspectorReport["issues"]): Finding[] {
  if (!Array.isArray(issues)) {
    return [];
  }
  return issues.map((issue, index) => {
    const id = String(issue.id ?? `skillspector:${index}`);
    const severity = mapSeverity(issue.severity);
    const path = issue.location?.file;
    const title = String(issue.finding ?? issue.id ?? "SkillSpector issue").slice(0, 200);
    const evidence = String(issue.explanation ?? issue.finding ?? "").slice(0, 400);
    return finding(`skillspector:${id}`, severity, title, path, evidence);
  });
}
