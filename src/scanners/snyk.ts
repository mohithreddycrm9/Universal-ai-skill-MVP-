import { spawnSync } from "node:child_process";
import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { runEnvelope } from "./helpers.js";

/** Snyk / mcp-scan adapter. Absence is ERROR/NOT_RUN, never PASS. */
export class SnykScanner implements SecurityScanner {
  readonly id = "snyk";
  readonly version = "adapter-1.0.0";

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const binary = typeof configuration.binary === "string" ? configuration.binary : "snyk";
    const found = spawnSync(binary, ["--version"], { encoding: "utf8", timeout: 3000 });
    if (found.error || found.status !== 0) {
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "NOT_RUN",
        [],
        `Snyk/mcp-scan not available. Not PASS. target=${target.skillId}`,
      );
    }
    return runEnvelope(
      this.id,
      found.stdout.trim() || this.version,
      this.clock,
      "INCONCLUSIVE",
      [],
      "Snyk present; adapter does not auto-PASS without a parsed report",
    );
  }
}
