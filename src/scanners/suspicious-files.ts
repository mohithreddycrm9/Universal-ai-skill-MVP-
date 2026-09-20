import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope, walkTarget } from "./helpers.js";

const PATTERNS: Array<{ id: string; re: RegExp; title: string; severity: "HIGH" | "CRITICAL" | "MEDIUM" }> = [
  { id: "curl-sh", re: /curl\s[^|\n]{0,80}\|\s*(ba)?sh/i, title: "Pipe-to-shell pattern", severity: "CRITICAL" },
  { id: "nc", re: /\bnc\s+-e\b|\/dev\/tcp\//i, title: "Reverse-shell-like string (simulated detection)", severity: "CRITICAL" },
  { id: "docker-sock", re: /docker\.sock/i, title: "Docker socket reference", severity: "CRITICAL" },
  { id: "rm-rf", re: /rm\s+-rf\s+\/\b/i, title: "Destructive filesystem string", severity: "HIGH" },
  { id: "eval-atob", re: /eval\s*\(\s*atob\s*\(|Buffer\.from\([^)]+,\s*['"]base64['"]/i, title: "Encoded payload execution pattern", severity: "HIGH" },
  { id: "passwd", re: /\/etc\/(shadow|passwd)/i, title: "Host credential file reference", severity: "HIGH" },
];

export class SuspiciousFilesScanner implements SecurityScanner {
  readonly id = "suspicious_files";
  readonly version = "1.0.0";

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, _configuration: ScannerConfig): Promise<ScannerRun> {
    const findings = [];
    for (const file of walkTarget(target)) {
      if (/\.(exe|dll|so|dylib)$/i.test(file.path)) {
        findings.push(finding("sus:binary", "HIGH", "Unexpected binary artifact", file.path, file.path));
      }
      for (const pattern of PATTERNS) {
        if (pattern.re.test(file.content)) {
          findings.push(finding(`sus:${pattern.id}`, pattern.severity, pattern.title, file.path, file.content.slice(0, 120)));
        }
      }
    }
    return runEnvelope(this.id, this.version, this.clock, findings.length ? "FAIL" : "PASS", findings);
  }
}
