import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope, walkTarget } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";

const CODE_PATTERNS: Array<{
  id: string;
  re: RegExp;
  severity: "LOW" | "MEDIUM" | "HIGH";
  title: string;
}> = [
  { id: "eval", re: /\beval\s*\(/g, severity: "HIGH", title: "Dynamic eval() usage" },
  { id: "inner-html", re: /dangerouslySetInnerHTML/g, severity: "MEDIUM", title: "dangerouslySetInnerHTML (XSS risk)" },
  { id: "hardcoded-password", re: /password\s*[:=]\s*['"][^'"]{4,}['"]/gi, severity: "HIGH", title: "Possible hardcoded password" },
  { id: "disable-eslint", re: /eslint-disable(?:-next-line)?\s+.*security/gi, severity: "MEDIUM", title: "ESLint security rule disabled" },
  { id: "insecure-http", re: /['"]http:\/\/(?!localhost|127\.0\.0\.1)/gi, severity: "LOW", title: "Cleartext http:// URL (non-localhost)" },
  { id: "md5-crypto", re: /\bcreateHash\s*\(\s*['"]md5['"]/gi, severity: "MEDIUM", title: "Weak hash (MD5)" },
  { id: "debugger", re: /\bdebugger\s*;/g, severity: "LOW", title: "debugger statement left in source" },
];

export class CodeHealthScanner implements SecurityScanner {
  readonly id = "code_health";
  readonly version = "1.0.0";
  readonly cost = COST_CATALOG.code_health;

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, _configuration: ScannerConfig): Promise<ScannerRun> {
    const findings = [];
    const paths = new Set(walkTarget(target).map((f) => f.path));
    const hasPackageJson = paths.has("package.json");
    const hasLock =
      paths.has("package-lock.json") ||
      paths.has("pnpm-lock.yaml") ||
      paths.has("yarn.lock") ||
      paths.has("bun.lockb");

    if (hasPackageJson && !hasLock) {
      findings.push(
        finding(
          "health:missing-lockfile",
          "MEDIUM",
          "package.json without a lockfile",
          "package.json",
          "Add package-lock.json, pnpm-lock.yaml, or yarn.lock for reproducible installs.",
        ),
      );
    }

    const envCommitted = [...paths].filter((p) => /^\.env(\.|$)/.test(p) || /\/\.env(\.|$)/.test(p));
    for (const envPath of envCommitted) {
      findings.push(
        finding(
          "health:env-committed",
          "HIGH",
          ".env file present in tree (secrets risk)",
          envPath,
          "Prefer .env.example and keep secrets out of version control.",
        ),
      );
    }

    if (!paths.has("README.md") && !paths.has("readme.md") && !paths.has("README")) {
      findings.push(
        finding(
          "health:no-readme",
          "LOW",
          "No README at repository root",
          undefined,
          "Document setup, security contacts, and how to run checks.",
        ),
      );
    }

    for (const file of walkTarget(target)) {
      for (const pattern of CODE_PATTERNS) {
        if (pattern.re.test(file.content)) {
          findings.push(
            finding(`health:${pattern.id}`, pattern.severity, pattern.title, file.path, file.content.slice(0, 120)),
          );
        }
        pattern.re.lastIndex = 0;
      }
    }

    const status =
      findings.some((f) => f.severity === "HIGH" || f.severity === "CRITICAL")
        ? "FAIL"
        : findings.some((f) => f.severity === "MEDIUM")
          ? "INCONCLUSIVE"
          : "PASS";

    const capped = findings.slice(0, 50);
    const notes =
      findings.length > capped.length
        ? `Reported ${capped.length} of ${findings.length} code-health findings.`
        : undefined;
    return runEnvelope(this.id, this.version, this.clock, status, capped, notes);
  }
}
