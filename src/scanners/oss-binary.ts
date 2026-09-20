import { existsSync, mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { Finding, ScanTarget, ScannerRun, SecurityStatus } from "../types.js";
import type { CostMetadata } from "../cost/types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope } from "./helpers.js";
import { defaultSpawn, missingBinary, type SpawnFn } from "../util/spawn.js";
import { assertNever } from "../util/assert-never.js";

export type OssScannerId = "semgrep" | "gitleaks" | "trivy" | "clamav" | "osv" | "syft";

/** Local OSS binary adapter. Missing binary → ERROR, never PASS. Invokes CLI when present. */
export class OssBinaryScanner implements SecurityScanner {
  private readonly clock: Clock;
  private readonly spawn: SpawnFn;
  private readonly binary: string;

  constructor(
    readonly id: OssScannerId,
    readonly version: string,
    readonly cost: CostMetadata,
    binary: string,
    clock?: Clock,
    spawn?: SpawnFn,
  ) {
    this.binary = binary;
    this.clock = clock ?? systemClock;
    this.spawn = spawn ?? defaultSpawn;
  }

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const binary = typeof configuration.binary === "string" ? configuration.binary : this.binary;
    const found = this.spawn(binary, ["--version"], { timeout: 4000 });
    if (missingBinary(found) || found.error || found.status !== 0) {
      return runEnvelope(
        this.id,
        this.version,
        this.clock,
        "ERROR",
        [],
        `${this.id} binary '${binary}' not available. Scan not performed. This is not PASS. target=${target.skillId}`,
      );
    }
    const toolVersion = found.stdout.trim().split("\n")[0] || this.version;
    const root = materializeTarget(target);
    const invocation = scanInvocation(this.id, root, configuration);
    const result = this.spawn(binary, invocation.args, { timeout: invocation.timeout, cwd: root });
    if (result.error?.code === "ETIMEDOUT") {
      return runEnvelope(
        this.id,
        toolVersion,
        this.clock,
        "TIMEOUT",
        [],
        `${this.id} timed out. TIMEOUT ≠ PASS.`,
      );
    }
    if (missingBinary(result)) {
      return runEnvelope(
        this.id,
        toolVersion,
        this.clock,
        "ERROR",
        [],
        `${this.id} could not be invoked. This is not PASS.`,
      );
    }
    const parsed = parseOssOutput(this.id, result.stdout, result.stderr, result.status);
    return runEnvelope(this.id, toolVersion, this.clock, parsed.status, parsed.findings, parsed.notes);
  }
}

function scanInvocation(
  id: OssScannerId,
  root: string,
  configuration: ScannerConfig,
): { args: string[]; timeout: number } {
  const extra = Array.isArray(configuration.args) ? configuration.args.map(String) : [];
  switch (id) {
    case "semgrep": {
      const rules =
        typeof configuration.rulesPath === "string"
          ? configuration.rulesPath
          : typeof configuration.config === "string"
            ? configuration.config
            : "config/semgrep-local.yml";
      return {
        args: extra.length ? extra : ["--metrics=off", "--offline", "--json", "--quiet", "--config", rules, root],
        timeout: 60_000,
      };
    }
    case "gitleaks":
      return {
        args: extra.length ? extra : ["detect", "--source", root, "--no-git", "--report-format", "json", "--no-banner"],
        timeout: 60_000,
      };
    case "trivy":
      return {
        args: extra.length ? extra : ["fs", "--offline-scan", "--skip-db-update", "--format", "json", "--quiet", root],
        timeout: 60_000,
      };
    case "clamav":
      return { args: extra.length ? extra : ["--no-summary", "-r", root], timeout: 60_000 };
    case "osv":
      return {
        args: extra.length ? extra : ["--offline", "--format", "json", "-r", root],
        timeout: 60_000,
      };
    case "syft":
      return { args: extra.length ? extra : ["scan", `dir:${root}`, "-o", "json"], timeout: 60_000 };
    default:
      return assertNever(id, "oss scanner id");
  }
}

function parseOssOutput(
  id: OssScannerId,
  stdout: string,
  stderr: string,
  status: number | null,
): { status: SecurityStatus; findings: Finding[]; notes: string } {
  const findings = extractFindings(id, stdout, stderr);
  const high = findings.some((item) => item.severity === "HIGH" || item.severity === "CRITICAL");
  if (high) {
    return {
      status: "FAIL",
      findings,
      notes: `${id} reported high/critical findings. Configured CLI check only; not a universal safety claim.`,
    };
  }
  if (status === 0 && findings.length === 0) {
    return {
      status: "PASS",
      findings,
      notes: `${id} CLI completed with no parsed findings. PASSED_CONFIGURED_CHECKS only; not a universal safety claim.`,
    };
  }
  if (status === 0) {
    return {
      status: "PASS",
      findings,
      notes: `${id} CLI completed. Findings below fail severity. Not a universal safety claim.`,
    };
  }
  if (findings.length) {
    return {
      status: "FAIL",
      findings,
      notes: `${id} CLI exited ${status} with findings. Not a universal safety claim.`,
    };
  }
  return {
    status: "INCONCLUSIVE",
    findings: [],
    notes: `${id} CLI exited ${status}. Could not parse a verdict. INCONCLUSIVE ≠ PASS. stderr=${clip(stderr)}`,
  };
}

function extractFindings(id: OssScannerId, stdout: string, stderr: string): Finding[] {
  const text = stdout.trim() || stderr.trim();
  if (!text.startsWith("{") && !text.startsWith("[")) {
    return [];
  }
  try {
    const json = JSON.parse(text) as unknown;
    switch (id) {
      case "semgrep":
        return semgrepFindings(json);
      case "gitleaks":
        return gitleaksFindings(json);
      case "trivy":
        return trivyFindings(json);
      case "osv":
        return osvFindings(json);
      case "syft":
        return [];
      case "clamav":
        return [];
      default:
        return assertNever(id, "oss scanner id");
    }
  } catch {
    return [];
  }
}

function semgrepFindings(json: unknown): Finding[] {
  const results = json && typeof json === "object" ? (json as { results?: unknown[] }).results : undefined;
  if (!Array.isArray(results)) {
    return [];
  }
  return results.slice(0, 50).map((row, index) => {
    const rec = row && typeof row === "object" ? (row as Record<string, unknown>) : {};
    const extra = rec.extra && typeof rec.extra === "object" ? (rec.extra as Record<string, unknown>) : {};
    const severity = mapSeverity(String(extra.severity ?? "MEDIUM"));
    return finding(`${String(rec.check_id ?? "semgrep")}:${index}`, severity, String(extra.message ?? rec.check_id ?? "finding"), String(rec.path ?? ""), String(extra.lines ?? ""));
  });
}

function gitleaksFindings(json: unknown): Finding[] {
  const rows = Array.isArray(json) ? json : json && typeof json === "object" ? (json as { findings?: unknown[]; leaks?: unknown[] }).findings ?? (json as { leaks?: unknown[] }).leaks : undefined;
  if (!Array.isArray(rows)) {
    return [];
  }
  return rows.slice(0, 50).map((row, index) => {
    const rec = row && typeof row === "object" ? (row as Record<string, unknown>) : {};
    return finding(`gitleaks:${index}`, "HIGH", String(rec.Description ?? rec.RuleID ?? "secret"), String(rec.File ?? ""), String(rec.Match ?? rec.Secret ?? "redacted"));
  });
}

function trivyFindings(json: unknown): Finding[] {
  const results = json && typeof json === "object" ? (json as { Results?: unknown[] }).Results : undefined;
  if (!Array.isArray(results)) {
    return [];
  }
  const findings: Finding[] = [];
  for (const result of results) {
    const vulns =
      result && typeof result === "object" ? (result as { Vulnerabilities?: unknown[] }).Vulnerabilities : undefined;
    if (!Array.isArray(vulns)) {
      continue;
    }
    for (const vuln of vulns.slice(0, 50)) {
      const rec = vuln && typeof vuln === "object" ? (vuln as Record<string, unknown>) : {};
      findings.push(
        finding(
          String(rec.VulnerabilityID ?? `trivy:${findings.length}`),
          mapSeverity(String(rec.Severity ?? "MEDIUM")),
          String(rec.Title ?? rec.VulnerabilityID ?? "vulnerability"),
          String((result as { Target?: string }).Target ?? ""),
          String(rec.Description ?? "").slice(0, 200),
        ),
      );
    }
  }
  return findings;
}

function osvFindings(json: unknown): Finding[] {
  const results = json && typeof json === "object" ? (json as { results?: unknown[] }).results : undefined;
  if (!Array.isArray(results)) {
    return [];
  }
  const findings: Finding[] = [];
  for (const row of results) {
    const packages = row && typeof row === "object" ? (row as { packages?: unknown[] }).packages : undefined;
    if (!Array.isArray(packages)) {
      continue;
    }
    for (const pkg of packages) {
      const vulns = pkg && typeof pkg === "object" ? (pkg as { vulnerabilities?: unknown[] }).vulnerabilities : undefined;
      if (!Array.isArray(vulns)) {
        continue;
      }
      for (const vuln of vulns) {
        const rec = vuln && typeof vuln === "object" ? (vuln as Record<string, unknown>) : {};
        findings.push(
          finding(String(rec.id ?? `osv:${findings.length}`), "HIGH", String(rec.summary ?? rec.id ?? "advisory"), undefined, String(rec.details ?? "").slice(0, 200)),
        );
      }
    }
  }
  return findings;
}

function mapSeverity(value: string): Finding["severity"] {
  const upper = value.toUpperCase();
  if (upper === "CRITICAL" || upper === "HIGH" || upper === "MEDIUM" || upper === "LOW") {
    return upper;
  }
  if (upper === "ERROR") {
    return "HIGH";
  }
  if (upper === "WARNING") {
    return "MEDIUM";
  }
  return "MEDIUM";
}

function materializeTarget(target: ScanTarget): string {
  if (target.quarantinePath && existsSync(target.quarantinePath)) {
    return target.quarantinePath;
  }
  const dir = mkdtempSync(join(tmpdir(), "skill-mcp-scan-"));
  mkdirSync(dir, { recursive: true });
  for (const file of target.package.files) {
    writeFileSync(join(dir, file.path.replaceAll("/", "_")), file.content, "utf8");
  }
  return dir;
}

function clip(text: string): string {
  const t = text.replace(/\s+/g, " ").trim();
  return t.length > 180 ? `${t.slice(0, 180)}…` : t;
}
