import { spawnSync } from "node:child_process";
import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { CostMetadata } from "../cost/types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { runEnvelope } from "./helpers.js";

/** Local OSS binary adapter. Missing binary → ERROR, never PASS. */
export class OssBinaryScanner implements SecurityScanner {
  constructor(
    readonly id: string,
    readonly version: string,
    readonly cost: CostMetadata,
    private readonly binary: string,
    private readonly clock: Clock = systemClock,
  ) {}

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const binary = typeof configuration.binary === "string" ? configuration.binary : this.binary;
    const found = spawnSync(binary, ["--version"], { encoding: "utf8", timeout: 3000 });
    if (found.error || found.status !== 0) {
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "ERROR",
        [],
        `${this.id} binary '${binary}' not available. Scan not performed. This is not PASS. target=${target.skillId}`,
      );
    }
    return runEnvelope(
      this.id,
      found.stdout.trim() || this.version,
      this.clock,
      "INCONCLUSIVE",
      [],
      `${this.id} binary present; this adapter does not treat a raw invocation as a universal safety verdict. INCONCLUSIVE ≠ PASS.`,
    );
  }
}
