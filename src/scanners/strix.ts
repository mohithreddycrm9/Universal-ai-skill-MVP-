import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { runEnvelope } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";
import { defaultSpawn, missingBinary, type SpawnFn } from "../util/spawn.js";

/**
 * Adapter for the open-source STRIX CLI from https://github.com/usestrix/strix
 * (Apache-2.0, PyPI: strix-agent). Missing binary → ERROR, never PASS.
 *
 * Real CLI shape: `strix --target <path>` (not `strix scan`).
 * Never invokes `strix cloud` (managed/paid platform).
 * Unparsed / non-zero exits stay INCONCLUSIVE — not a universal safety claim.
 *
 * Runtime needs Docker + an LLM key (see docs/STRIX.md). Under $0 policy use a
 * free/local LLM; commercial LLM usage is outside this MCP's free tier.
 */
export class StrixScanner implements SecurityScanner {
  readonly id = "strix";
  readonly version = "adapter-1.1.0-usestrix";
  readonly cost = COST_CATALOG.strix;
  private readonly clock: Clock;
  private readonly spawn: SpawnFn;

  constructor(clock?: Clock, spawn?: SpawnFn) {
    this.clock = clock ?? systemClock;
    this.spawn = spawn ?? defaultSpawn;
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

    // LLM env: without a model/key STRIX cannot run; do not pretend it passed.
    const llmModel = process.env.STRIX_LLM || process.env.LLM_MODEL || "";
    const llmKey = process.env.LLM_API_KEY || process.env.OPENAI_API_KEY || "";
    if (!llmModel && !llmKey) {
      return runEnvelope(
        this.id,
        version,
        this.clock,
        "ERROR",
        [],
        "STRIX OSS needs STRIX_LLM / LLM_API_KEY for a model provider. For $0 use a free/local model. Scan not performed. Not PASS. See docs/STRIX.md.",
      );
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
