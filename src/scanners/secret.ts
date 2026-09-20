import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope, walkTarget } from "./helpers.js";

const PATTERNS: Array<{ id: string; re: RegExp; title: string }> = [
  { id: "aws-access-key", re: /AKIA[0-9A-Z]{16}/g, title: "Possible cloud access key" },
  { id: "github-pat", re: /ghp_[A-Za-z0-9]{20,}/g, title: "Possible GitHub token" },
  { id: "github-fine-pat", re: /github_pat_[A-Za-z0-9_]{20,}/g, title: "Possible GitHub fine-grained token" },
  { id: "pem", re: /-----BEGIN [A-Z ]*PRIVATE KEY-----/g, title: "Private key material" },
  { id: "slack", re: /xox[baprs]-[A-Za-z0-9-]{10,}/g, title: "Possible chat token" },
  { id: "google-api", re: /AIza[0-9A-Za-z\-_]{35}/g, title: "Possible Google API key" },
];

export class SecretScanner implements SecurityScanner {
  readonly id = "secret";
  readonly version = "1.0.0";

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, _configuration: ScannerConfig): Promise<ScannerRun> {
    const findings = [];
    for (const file of walkTarget(target)) {
      for (const pattern of PATTERNS) {
        if (pattern.re.test(file.content)) {
          findings.push(finding(`secret:${pattern.id}`, "CRITICAL", pattern.title, file.path, file.content.slice(0, 80)));
        }
        pattern.re.lastIndex = 0;
      }
    }
    return runEnvelope(this.id, this.version, this.clock, findings.length ? "FAIL" : "PASS", findings);
  }
}
