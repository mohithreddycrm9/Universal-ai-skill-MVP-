import { describe, expect, it } from "vitest";
import { inferRisk, maxRisk } from "../../src/skills/manifest.js";
import { loadConfig } from "../../src/policy/load.js";
import { benignPackage } from "../helpers.js";
import type { SkillPackage } from "../../src/types.js";

function pkgWith(files: SkillPackage["files"], commit = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"): SkillPackage {
  return {
    ...benignPackage(commit),
    repository: "fixture-org/risk-fixture",
    repositoryUrl: "https://example.local/fixture-org/risk-fixture",
    files,
  };
}

describe("inferRisk — declared may raise only", () => {
  it("postinstall + entrypoint + declared LOW → still HIGH (≥ heuristic)", () => {
    const pkg = pkgWith([
      {
        path: "SKILL.md",
        content: `name: risky
risk: LOW
entrypoints:
  - index.js
instructions: do things
`,
      },
      {
        path: "package.json",
        content: JSON.stringify({
          name: "risky",
          version: "1.0.0",
          scripts: { postinstall: "echo hi" },
        }),
      },
    ]);
    expect(inferRisk(pkg)).toBe("HIGH");
  });

  it("HIGH heuristic + LOW declared → HIGH", () => {
    const pkg = pkgWith([
      {
        path: "SKILL.md",
        content: `name: hooks
risk: LOW
entrypoints:
  - run.sh
instructions: install helpers
`,
      },
      {
        path: "package.json",
        content: JSON.stringify({ scripts: { install: "node setup.js" } }),
      },
    ]);
    expect(inferRisk(pkg)).toBe("HIGH");
  });

  it("LOW heuristic + CRITICAL declared → CRITICAL", () => {
    const pkg = pkgWith([
      {
        path: "SKILL.md",
        content: `name: docs
risk: CRITICAL
instructions: read only docs
`,
      },
      { path: "package.json", content: JSON.stringify({ name: "docs", version: "1.0.0" }) },
    ]);
    expect(inferRisk(pkg)).toBe("CRITICAL");
  });

  it("malformed declared risk is ignored (safe)", () => {
    const pkg = pkgWith([
      {
        path: "SKILL.md",
        content: `name: docs
risk: NOT_A_REAL_LEVEL
instructions: read only
`,
      },
    ]);
    expect(inferRisk(pkg)).toBe("LOW");
  });

  it("aggregates declared and inferred risk with maxRisk", () => {
    expect(maxRisk("HIGH", "LOW")).toBe("HIGH");
    expect(maxRisk("LOW", "CRITICAL")).toBe("CRITICAL");
    const config = loadConfig("config");
    expect(config.security.extendedScanningEnabled).toBe(false);
    expect(config.security.optionalScanners).not.toContain("skillspector");
  });
});
