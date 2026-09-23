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
  /** When set, only these scanner ids are executed (still subject to enabled config). */
  onlyScannerIds?: readonly string[];
  /** When set, overrides config.security.requiredScanners for federate (e.g. workspace health). */
  requiredScannerIds?: readonly string[];
  /** Run these scanners even when disabled in scanner-policy (health-watch profile). */
  forceScannerIds?: readonly string[];
}

export class SecurityOrchestrator {
  constructor(
    private readonly scanners: SecurityScanner[],
    private readonly config: AppConfig,
    private readonly clock: Clock,
  ) {}

  async scan(target: ScanTarget, risk: RiskLevel, options: OrchestratorScanOptions = {}): Promise<FederatedSecurityResult> {
    const only = options.onlyScannerIds ? new Set(options.onlyScannerIds) : null;
    const force = options.forceScannerIds ? new Set(options.forceScannerIds) : null;
    const enabled = this.scanners.filter((scanner) => {
      if (only && !only.has(scanner.id)) {
        return false;
      }
      const cfg = this.config.scanners.scanners[scanner.id];
      if (force?.has(scanner.id)) {
        return true;
      }
      return cfg?.enabled !== false;
    });
    const allowPaid = new Set(options.allowPaidScannerIds ?? []);
    const concurrency = Math.max(1, this.config.security.maxConcurrentScanners ?? 4);
    const defaultTimeoutMs = this.config.security.scannerTimeoutMs ?? 120_000;
    const tasks = enabled.map((scanner) => async (): Promise<ScannerRun> => {
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
      const timeoutMs =
        typeof cfg.timeoutMs === "number" && cfg.timeoutMs > 0 ? cfg.timeoutMs : defaultTimeoutMs;
      try {
        return await withTimeout(
          scanner.scan(target, { ...cfg, costPolicy: this.config.cost, timeoutMs }),
          timeoutMs,
          scanner.id,
          scanner.version,
          this.clock,
        );
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
    });
    const runs = await mapPool(tasks, concurrency);
    return federate(runs, risk, this.config, {
      requiredScannerIds: options.requiredScannerIds,
    });
  }
}

export interface FederateOptions {
  requiredScannerIds?: readonly string[];
}

/**
 * Federate scanner runs into a single gate status.
 *
 * - Every executed scanner (status !== NOT_RUN) contributes to the aggregate.
 * - FAIL or findings at/above failOnSeverity → aggregate FAIL.
 * - ERROR / TIMEOUT / INCONCLUSIVE → aggregate INCONCLUSIVE (unless already FAIL).
 * - requiredScanners (+ strix when risk requires it) are coverage only:
 *   missing / NOT_RUN required → incomplete → INCONCLUSIVE (never PASS).
 * - optionalScanners may run and contribute when executed; absence is not a gap.
 * - Commercial / $0-blocked scanners stay NOT_RUN (never PASS).
 * - Disabled scanners are omitted from runs and have no effect.
 */
export function federate(
  runs: ScannerRun[],
  risk: RiskLevel,
  config: AppConfig,
  options: FederateOptions = {},
): FederatedSecurityResult {
  const required = new Set(
    options.requiredScannerIds ?? config.security.requiredScanners,
  );
  if (!options.requiredScannerIds && isStrixRequired(risk, config.security)) {
    required.add("strix");
  }
  // optionalScanners: may execute and contribute to the aggregate, but their
  // absence / NOT_RUN never creates a coverage gap (unlike requiredScanners).
  const optional = new Set(config.security.optionalScanners);
  void optional;

  const byId = new Map(runs.map((run) => [run.scannerId, run]));
  const requiredMissing: string[] = [];
  let fail = false;
  let inconclusive = false;

  // Coverage completeness — required scanners only.
  for (const id of required) {
    const run = byId.get(id);
    if (!run || run.status === "NOT_RUN") {
      requiredMissing.push(id);
      inconclusive = true;
    }
  }

  // Every *executed* scanner contributes (required, optional, or ad-hoc).
  // Commercial/$0 blocked scanners remain NOT_RUN and do not count as PASS.
  for (const run of runs) {
    if (run.status === "NOT_RUN") {
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

  // Deterministic strongest result: FAIL > INCONCLUSIVE > PASS.
  const status: SecurityStatus = fail
    ? "FAIL"
    : inconclusive || requiredMissing.length
      ? "INCONCLUSIVE"
      : "PASS";
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

async function mapPool<T>(tasks: Array<() => Promise<T>>, concurrency: number): Promise<T[]> {
  const results: T[] = new Array(tasks.length);
  let next = 0;
  async function worker(): Promise<void> {
    for (;;) {
      const i = next++;
      if (i >= tasks.length) return;
      results[i] = await tasks[i]!();
    }
  }
  const workers = Array.from({ length: Math.min(concurrency, tasks.length) }, () => worker());
  await Promise.all(workers);
  return results;
}

async function withTimeout(
  promise: Promise<ScannerRun>,
  timeoutMs: number,
  scannerId: string,
  scannerVersion: string,
  clock: Clock,
): Promise<ScannerRun> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    return await Promise.race([
      promise,
      new Promise<ScannerRun>((resolve) => {
        timer = setTimeout(() => {
          resolve({
            scannerId,
            scannerVersion,
            status: "TIMEOUT",
            findings: [],
            notes: `Scanner '${scannerId}' exceeded orchestrator timeout (${timeoutMs}ms). TIMEOUT ≠ PASS.`,
            startedAt: iso(clock),
            finishedAt: iso(clock),
          });
        }, timeoutMs);
      }),
    ]);
  } finally {
    if (timer) clearTimeout(timer);
  }
}
