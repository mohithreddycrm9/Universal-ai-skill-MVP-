import type { AppConfig } from "../policy/load.js";
import { isStrixRequired } from "../policy/load.js";
import type { SecurityScanner } from "../scanners/types.js";
import type { Clock } from "../util/clock.js";
import { iso } from "../util/clock.js";
import type { FindingSeverity, RiskLevel, ScanTarget, ScannerRun, SecurityStatus } from "../types.js";
import { assertNever } from "../util/assert-never.js";
import { isCommercial } from "../cost/types.js";

export interface FederatedSecurityResult {
  status: SecurityStatus;
  scanners: ScannerRun[];
  claim: string;
  requiredMissing: string[];
}

export interface OrchestratorScanOptions {
  /** Commercial scanners never run unless their ids are listed here. */
  allowPaidScannerIds?: string[];
}

export class SecurityOrchestrator {
  constructor(
    private readonly scanners: SecurityScanner[],
    private readonly config: AppConfig,
    private readonly clock: Clock,
  ) {}

  async scan(target: ScanTarget, risk: RiskLevel, options: OrchestratorScanOptions = {}): Promise<FederatedSecurityResult> {
    const enabled = this.scanners.filter((scanner) => {
      const cfg = this.config.scanners.scanners[scanner.id];
      return cfg?.enabled !== false;
    });
    const allowPaid = new Set(options.allowPaidScannerIds ?? []);
    const runs = await Promise.all(
      enabled.map(async (scanner) => {
        const cfg = this.config.scanners.scanners[scanner.id] ?? { enabled: true };
        const commercial = isCommercial(scanner.cost);
        if (commercial && (this.config.cost.neverAutoPaidFallback || !allowPaid.has(scanner.id))) {
          return {
            scannerId: scanner.id,
            scannerVersion: scanner.version,
            status: "NOT_RUN" as const,
            findings: [],
            notes: `Commercial scanner '${scanner.id}' skipped. Not an automatic paid fallback. Not PASS. Free alternative: ${scanner.cost.freeAlternative ?? "built-in OSS scanners"}.`,
            startedAt: iso(this.clock),
            finishedAt: iso(this.clock),
          };
        }
        try {
          return await scanner.scan(target, cfg);
        } catch (error) {
          return {
            scannerId: scanner.id,
            scannerVersion: scanner.version,
            status: "ERROR" as const,
            findings: [],
            notes: error instanceof Error ? error.message : "scanner threw",
            startedAt: iso(this.clock),
            finishedAt: iso(this.clock),
          };
        }
      }),
    );
    return federate(runs, risk, this.config);
  }
}

export function federate(runs: ScannerRun[], risk: RiskLevel, config: AppConfig): FederatedSecurityResult {
  const required = new Set(config.security.requiredScanners);
  if (isStrixRequired(risk, config.security)) {
    required.add("strix");
  }
  const byId = new Map(runs.map((run) => [run.scannerId, run]));
  const requiredMissing: string[] = [];
  let fail = false;
  let inconclusive = false;

  for (const id of required) {
    const run = byId.get(id);
    if (!run || run.status === "NOT_RUN") {
      requiredMissing.push(id);
      inconclusive = true;
      continue;
    }
    switch (run.status) {
      case "FAIL":
        fail = true;
        break;
      case "INCONCLUSIVE":
      case "ERROR":
      case "TIMEOUT":
        inconclusive = true;
        break;
      case "PASS":
        break;
      default:
        assertNever(run.status, "scanner status");
    }
    if (hasFailSeverity(run, config.security.failOnSeverity)) {
      fail = true;
    }
  }

  const status: SecurityStatus = fail ? "FAIL" : inconclusive || requiredMissing.length ? "INCONCLUSIVE" : "PASS";
  const claim =
    status === "PASS"
      ? "PASSED_CONFIGURED_CHECKS"
      : status === "FAIL"
        ? "FAILED"
        : "INCONCLUSIVE";
  return { status, scanners: runs, claim, requiredMissing };
}

function hasFailSeverity(run: ScannerRun, failOn: FindingSeverity[]): boolean {
  return run.findings.some((finding) => failOn.includes(finding.severity));
}
