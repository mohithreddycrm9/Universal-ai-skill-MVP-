import { spawnSync } from "node:child_process";
import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { runEnvelope } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";

/**
 * STRIX adapter. If the binary is missing or fails, status is ERROR — never PASS.
 */
export class StrixScanner implements SecurityScanner {
  readonly id = "strix";
  readonly version = "adapter-1.0.0";
  readonly cost = COST_CATALOG.strix;

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const binary = typeof configuration.binary === "string" ? configuration.binary : "strix";
    const failOpen = configuration.failOpen === true;
    const found = spawnSync(binary, ["--version"], { encoding: "utf8", timeout: 3000 });
    if (found.error || found.status !== 0) {
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
    return runEnvelope(
      this.id,
      found.stdout.trim() || this.version,
      this.clock,
      "INCONCLUSIVE",
      [],
      "STRIX binary present but this adapter does not treat a raw STRIX run as a universal safety verdict; result stays INCONCLUSIVE without a parsed report",
    );
  }
}
