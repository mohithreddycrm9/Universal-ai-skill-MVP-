import type { Clock } from "../util/clock.js";
import { iso } from "../util/clock.js";
import { redactValue } from "../util/redact.js";
import type { Finding, ScanTarget, ScannerRun, SecurityStatus } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";

export function runEnvelope(
  scannerId: string,
  scannerVersion: string,
  clock: Clock,
  status: SecurityStatus,
  findings: Finding[],
  notes?: string,
): ScannerRun {
  const startedAt = iso(clock);
  return {
    scannerId,
    scannerVersion,
    status,
    findings,
    notes,
    startedAt,
    finishedAt: startedAt,
  };
}

export function clip(text: string, max = 480): string {
  const redacted = text.replace(/AKIA[0-9A-Z]{16}/g, "AKIA…[REDACTED]")
    .replace(/ghp_[A-Za-z0-9]{20,}/g, "ghp_…[REDACTED]")
    .replace(/github_pat_[A-Za-z0-9_]{20,}/g, "github_pat_…[REDACTED]")
    .replace(/-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g, "[REDACTED PRIVATE KEY]");
  if (redacted.length <= max) {
    return redacted;
  }
  return `${redacted.slice(0, max - 14)}…[truncated]`;
}

export function finding(
  id: string,
  severity: Finding["severity"],
  title: string,
  path: string | undefined,
  evidence: string,
): Finding {
  return { id, severity, title, path, evidence: clip(evidence.includes("BEGIN") ? redactValue(evidence) : evidence) };
}

export function walkTarget(target: ScanTarget): Array<{ path: string; content: string }> {
  const files = target.package.files;
  if (target.changedPaths?.length) {
    const set = new Set(target.changedPaths);
    const subset = files.filter((file) => set.has(file.path));
    return subset.length > 0 ? subset : files;
  }
  return files;
}
