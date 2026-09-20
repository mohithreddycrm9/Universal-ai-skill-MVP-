import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope, walkTarget } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";

const POPULAR = [
  "lodash",
  "express",
  "react",
  "typescript",
  "vite",
  "eslint",
  "webpack",
  "axios",
  "next",
  "mocha",
];

export class DependencyScanner implements SecurityScanner {
  readonly id = "dependency";
  readonly version = "1.0.0";
  readonly cost = COST_CATALOG.dependency;

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const findings = [];
    const distance = typeof configuration.typosquatDistance === "number" ? configuration.typosquatDistance : 1;
    const components: Array<{ name: string; version: string }> = [];
    for (const file of walkTarget(target)) {
      if (!/(^|\/)package\.json$/i.test(file.path)) {
        continue;
      }
      try {
        const parsed = JSON.parse(file.content) as {
          dependencies?: Record<string, string>;
          devDependencies?: Record<string, string>;
          scripts?: Record<string, string>;
        };
        const scripts = parsed.scripts ?? {};
        for (const hook of ["preinstall", "install", "postinstall"]) {
          if (scripts[hook]) {
            findings.push(
              finding("dep:install-hook", "HIGH", `package.json ${hook} script present`, file.path, scripts[hook] ?? ""),
            );
          }
        }
        const deps = { ...(parsed.dependencies ?? {}), ...(parsed.devDependencies ?? {}) };
        for (const [name, version] of Object.entries(deps)) {
          components.push({ name, version });
          const squat = nearestTypo(name, distance);
          if (squat) {
            findings.push(
              finding("dep:typosquat", "HIGH", `Possible typosquat of ${squat}`, file.path, name),
            );
          }
        }
      } catch {
        findings.push(finding("dep:parse", "MEDIUM", "package.json could not be parsed", file.path, "parse error"));
      }
    }
    const sbom = {
      bomFormat: "CycloneDX",
      specVersion: "1.5",
      components: components.slice(0, 40),
    };
    const notes = `sbom_components=${components.length}; summary_only`;
    const status = findings.some((item) => item.severity === "HIGH" || item.severity === "CRITICAL") ? "FAIL" : "PASS";
    return {
      ...runEnvelope(this.id, this.version, this.clock, status, findings, notes),
      findings,
      notes: `${notes}; ${JSON.stringify(sbom).slice(0, 400)}`,
    };
  }
}

function nearestTypo(name: string, maxDistance: number): string | undefined {
  for (const popular of POPULAR) {
    if (name === popular) {
      continue;
    }
    if (levenshtein(name, popular) === maxDistance) {
      return popular;
    }
  }
  return undefined;
}

function levenshtein(a: string, b: string): number {
  const rows = a.length + 1;
  const cols = b.length + 1;
  const dp: number[][] = Array.from({ length: rows }, () => Array<number>(cols).fill(0));
  for (let i = 0; i < rows; i++) {
    dp[i]![0] = i;
  }
  for (let j = 0; j < cols; j++) {
    dp[0]![j] = j;
  }
  for (let i = 1; i < rows; i++) {
    for (let j = 1; j < cols; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i]![j] = Math.min(dp[i - 1]![j]! + 1, dp[i]![j - 1]! + 1, dp[i - 1]![j - 1]! + cost);
    }
  }
  return dp[a.length]![b.length]!;
}
