import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { runEnvelope } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";
import { CostDetector } from "../cost/detector.js";
import { costMetadataForStrixLlm, inspectStrixLlm } from "../cost/strix-llm.js";
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

/**
 * Adapter for the open-source STRIX CLI from https://github.com/usestrix/strix
 * (Apache-2.0, PyPI: strix-agent). Missing binary → ERROR, never PASS.
 *
 * Real CLI shape: `strix --target <path>` (not `strix scan`).
 * Never invokes `strix cloud` (managed/paid platform).
 * Unparsed / non-zero exits stay INCONCLUSIVE — not a universal safety claim.
 *
 * Runtime needs Docker + an LLM. Under ALLOW_FREE_ONLY / DENY_ALL_PAID, the
 * configured LLM must be proven local/free before any `strix --target` spawn
 * (software is free; external LLM calls may be chargeable).
 */
export class StrixScanner implements SecurityScanner {
  readonly id = "strix";
  readonly version = "adapter-1.1.0-usestrix";
  readonly cost = COST_CATALOG.strix;
  private readonly clock: Clock;
  private readonly spawn: SpawnFn;
  private readonly defaultCostPolicy: CostPolicy;

  constructor(clock?: Clock, spawn?: SpawnFn, costPolicy?: CostPolicy) {
    this.clock = clock ?? systemClock;
    this.spawn = spawn ?? defaultSpawn;
    this.defaultCostPolicy = costPolicy ?? DEFAULT_FREE_ONLY;
  }

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const binary = typeof configuration.binary === "string" ? configuration.binary : "strix";
    const failOpen = configuration.failOpen === true;
    const quarantine = target.quarantinePath || ".";

    // Refuse cloud / managed platform entrypoints even if someone puts them in args.
    const configuredArgs = Array.isArray(configuration.args) ? configuration.args.map(String) : null;
    if (configuredArgs?.[0] === "cloud") {
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "ERROR",
        [],
        "Refusing `strix cloud` (managed platform). Use local OSS CLI only. See docs/STRIX.md. Not PASS.",
      );
    }

    const found = this.spawn(binary, ["--version"], { timeout: 4000 });
    if (missingBinary(found) || found.error || found.status !== 0) {
      if (failOpen) {
        return runEnvelope(
          this.id,
          this.version,
          this.clock,
          "INCONCLUSIVE",
          [],
          "STRIX binary unavailable; failOpen is INCONCLUSIVE, never PASS. Install from https://github.com/usestrix/strix",
        );
      }
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "ERROR",
        [],
        `STRIX not available (${binary}). Install OSS CLI from https://github.com/usestrix/strix (Apache-2.0). Scan not performed. This is not PASS. fingerprint=${target.skillId}`,
      );
    }
    const version = found.stdout.trim().split("\n")[0] || this.version;

    // OSS STRIX needs Docker for its sandbox; missing Docker → ERROR (never PASS).
    const docker = this.spawn("docker", ["info"], { timeout: 5000 });
    const dockerOk = !missingBinary(docker) && !docker.error && docker.status === 0;
    if (!dockerOk) {
      return runEnvelope(
        this.id,
        version,
        this.clock,
        "ERROR",
        [],
        "STRIX OSS requires a running local Docker engine (usestrix/strix). Docker missing/unavailable. Not PASS.",
      );
    }

    // Classify LLM cost BEFORE any `strix --target` (may call external providers).
    const inspection = inspectStrixLlm({ env: process.env, config: configuration });
    if (inspection.class === "unknown" && !inspection.model && !inspection.provider && !inspection.hasApiKey) {
      return runEnvelope(
        this.id,
        version,
        this.clock,
        "ERROR",
        [],
        "STRIX OSS needs STRIX_LLM / LLM_API_KEY for a model provider. For $0 use a free/local model. Scan not performed. Not PASS. See docs/STRIX.md.",
      );
    }

    const llmCost = costMetadataForStrixLlm(inspection);
    const policy =
      configuration.costPolicy && typeof configuration.costPolicy === "object"
        ? (configuration.costPolicy as CostPolicy)
        : this.defaultCostPolicy;
    const detector = new CostDetector(policy);
    const approvalStatus = configuration.costApprovalStatus;
    const approvalId =
      typeof configuration.costApprovalId === "string" ? configuration.costApprovalId : undefined;
    const decision = detector.evaluate(
      "scan_skill:strix_llm",
      llmCost,
      approvalStatus === "APPROVED" && approvalId
        ? { approvalStatus: "APPROVED", approvalId }
        : approvalStatus === "REJECTED"
          ? { approvalStatus: "REJECTED" }
          : {},
    );

    if (!decision.proceed) {
      const policyName = policy.policy;
      const note =
        decision.kind === "NEEDS_APPROVAL"
          ? `STRIX LLM not proven local/free (${inspection.class}). ${inspection.reason} Policy ${policyName} requires explicit approval before a potentially chargeable LLM call. No external LLM request was made. NOT_RUN ≠ PASS. Free alternative: local/ollama/lmstudio model or SKILL_MCP_STRIX_LLM_IS_FREE=1.`
          : `STRIX blocked by cost policy ${policyName}: ${decision.reason} LLM class=${inspection.class}. ${inspection.reason} No external LLM request was made. ALLOW_FREE_ONLY/DENY blocks potentially chargeable LLM. NOT_RUN ≠ PASS.`;
      return runEnvelope(this.id, version, this.clock, "NOT_RUN", [], note);
    }

    const args = configuredArgs ?? ["--target", quarantine];
    const result = this.spawn(binary, args, {
      timeout: typeof configuration.timeoutMs === "number" ? configuration.timeoutMs : 120_000,
    });
    if (result.error?.code === "ETIMEDOUT") {
      return runEnvelope(this.id, version, this.clock, "TIMEOUT", [], "STRIX timed out. TIMEOUT ≠ PASS.");
    }
    if (result.status === 0) {
      return runEnvelope(
        this.id,
        version,
        this.clock,
        "PASS",
        [],
        "Local usestrix/strix CLI exited 0 for this target. PASSED_CONFIGURED_CHECKS only; not a universal safety claim.",
      );
    }
    return runEnvelope(
      this.id,
      version,
      this.clock,
      "INCONCLUSIVE",
      [],
      `STRIX CLI exited ${result.status}. Adapter does not treat raw output as a universal safety verdict. INCONCLUSIVE ≠ PASS.`,
    );
  }
}
