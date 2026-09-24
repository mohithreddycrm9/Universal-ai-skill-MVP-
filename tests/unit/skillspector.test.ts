import { describe, expect, it } from "vitest";
import { SkillspectorScanner } from "../../src/scanners/skillspector.js";
import { secretPackage, injectionPackage } from "../helpers.js";
import type { ScanTarget } from "../../src/types.js";
import type { SpawnFn } from "../../src/util/spawn.js";

function target(pkg: ReturnType<typeof secretPackage>, quarantine = "/tmp/q"): ScanTarget {
  return { skillId: "skl_test", package: pkg, quarantinePath: quarantine };
}

const benignJson = JSON.stringify({
  risk_assessment: { score: 0, severity: "LOW", recommendation: "SAFE" },
  issues: [],
  metadata: { skillspector_version: "2.12.0" },
});

const maliciousJson = JSON.stringify({
  risk_assessment: { score: 61, severity: "HIGH", recommendation: "DO_NOT_INSTALL" },
  issues: [
    {
      id: "P1",
      severity: "HIGH",
      finding: "Ignore previous instructions",
      explanation: "Instruction override",
      location: { file: "README.md" },
    },
  ],
  metadata: { skillspector_version: "2.12.0" },
});

describe("SkillSpector scanner", () => {
  it("errors when binary is missing", async () => {
    const spawn: SpawnFn = () => ({ status: 127, stdout: "", stderr: "not found" });
    const run = await new SkillspectorScanner(undefined, spawn).scan(target(secretPackage()), {
      binary: "skillspector-missing",
      failOpen: false,
    });
    expect(run.status).toBe("ERROR");
    expect(run.status).not.toBe("PASS");
  });

  it("passes clean static report", async () => {
    const spawn: SpawnFn = (cmd, args) => {
      if (args[0] === "--version") return { status: 0, stdout: "skillspector 2.12.0\n", stderr: "" };
      if (args[0] === "scan") return { status: 0, stdout: benignJson, stderr: "" };
      return { status: 1, stdout: "", stderr: "" };
    };
    const run = await new SkillspectorScanner(undefined, spawn).scan(target(secretPackage()), {
      binary: "skillspector",
      useLlm: false,
    });
    expect(run.status).toBe("PASS");
    expect(run.findings).toHaveLength(0);
  });

  it("fails on DO_NOT_INSTALL and HIGH issues", async () => {
    let scanArgs: readonly string[] = [];
    const spawn: SpawnFn = (cmd, args) => {
      if (args[0] === "--version") return { status: 0, stdout: "skillspector 2.12.0\n", stderr: "" };
      if (args[0] === "scan") {
        scanArgs = args;
        return { status: 1, stdout: maliciousJson, stderr: "" };
      }
      return { status: 1, stdout: "", stderr: "" };
    };
    const run = await new SkillspectorScanner(undefined, spawn).scan(target(injectionPackage()), {
      binary: "skillspector",
      useLlm: false,
    });
    expect(run.status).toBe("FAIL");
    expect(run.findings.length).toBeGreaterThan(0);
    expect(scanArgs).toContain("--no-llm");
  });

  it("blocks LLM scan under ALLOW_FREE_ONLY without approval", async () => {
    const spawn: SpawnFn = (cmd, args) => {
      if (args[0] === "--version") return { status: 0, stdout: "skillspector 2.12.0\n", stderr: "" };
      return { status: 0, stdout: benignJson, stderr: "" };
    };
    const run = await new SkillspectorScanner(undefined, spawn).scan(target(secretPackage()), {
      binary: "skillspector",
      useLlm: true,
    });
    expect(run.status).toBe("NOT_RUN");
  });
});
