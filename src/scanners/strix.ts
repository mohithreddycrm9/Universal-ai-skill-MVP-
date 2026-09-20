import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { runEnvelope } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";
import { defaultSpawn, missingBinary, type SpawnFn } from "../util/spawn.js";

/**
 * STRIX adapter. Missing binary → ERROR, never PASS.
 * When present, invokes the CLI; unparsed output stays INCONCLUSIVE.
 */
export class StrixScanner implements SecurityScanner {
  readonly id = "strix";
  readonly version = "adapter-1.0.0";
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
    const found = this.spawn(binary, ["--version"], { timeout: 4000 });
    if (missingBinary(found) || found.error || found.status !== 0) {
      if (failOpen) {
        return runEnvelope(
          this.id,
          this.version,
          this.clock,
          "INCONCLUSIVE",
          [],
          "STRIX binary unavailable; failOpen is INCONCLUSIVE, never PASS",
        );
      }
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "ERROR",
        [],
        `STRIX not available (${binary}). Scan not performed. This is not PASS. fingerprint=${target.skillId}`,
      );
    }
    const version = found.stdout.trim().split("\n")[0] || this.version;
    const args = Array.isArray(configuration.args)
      ? configuration.args.map(String)
      : ["scan", target.quarantinePath || "."];
    const result = this.spawn(binary, args, { timeout: 60_000 });
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
        "STRIX CLI exited 0. PASSED_CONFIGURED_CHECKS only; not a universal safety claim.",
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
