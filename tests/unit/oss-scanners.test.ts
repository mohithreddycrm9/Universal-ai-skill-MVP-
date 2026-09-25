import { describe, expect, it } from "vitest";
import { OssBinaryScanner } from "../../src/scanners/oss-binary.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import { secretPackage } from "../helpers.js";
import type { ScanTarget } from "../../src/types.js";
import type { SpawnFn, SpawnResult } from "../../src/util/spawn.js";

function target(): ScanTarget {
  return { skillId: "skl_test", package: secretPackage(), quarantinePath: "/tmp/skill-mcp-q-missing" };
}

function enoent(): SpawnResult {
  return { status: 1, stdout: "", stderr: "", error: Object.assign(new Error("missing"), { code: "ENOENT" }) };
}

describe("OSS CLI scanners", () => {
  it("returns ERROR (never PASS) when binaries are missing", async () => {
    const spawn: SpawnFn = () => enoent();
    for (const id of ["semgrep", "gitleaks", "trivy", "osv", "syft"] as const) {
      const scanner = new OssBinaryScanner(id, "adapter-1.0.0", COST_CATALOG[id], id, undefined, spawn);
      const run = await scanner.scan(target(), { enabled: true });
      expect(run.status).toBe("ERROR");
      expect(run.status).not.toBe("PASS");
    }
  });

  it("invokes the CLI when present and PASSes configured checks with no findings", async () => {
    const spawn: SpawnFn = (_cmd, args) => {
      if (args[0] === "--version") {
        return { status: 0, stdout: "semgrep 1.2.3\n", stderr: "" };
      }
      expect(args).toContain("--offline");
      expect(args).toContain("--json");
      return { status: 0, stdout: JSON.stringify({ results: [] }), stderr: "" };
    };
    const scanner = new OssBinaryScanner("semgrep", "adapter-1.0.0", COST_CATALOG.semgrep, "semgrep", undefined, spawn);
    const run = await scanner.scan(target(), { enabled: true, rulesPath: "config/semgrep-local.yml" });
    expect(run.status).toBe("PASS");
    expect(run.notes ?? "").toMatch(/not a universal safety claim/i);
  });

  it("fails closed on high/critical parsed findings", async () => {
    const spawn: SpawnFn = (_cmd, args) => {
      if (args[0] === "--version") {
        return { status: 0, stdout: "gitleaks 8\n", stderr: "" };
      }
      return {
        status: 1,
        stdout: JSON.stringify({ findings: [{ Description: "aws key", File: "config.txt", Match: "AKIAIOSFODNN7EXAMPLE" }] }),
        stderr: "",
      };
    };
    const scanner = new OssBinaryScanner("gitleaks", "adapter-1.0.0", COST_CATALOG.gitleaks, "gitleaks", undefined, spawn);
    const run = await scanner.scan(target(), { enabled: true });
    expect(run.status).toBe("FAIL");
    expect(JSON.stringify(run.findings)).not.toMatch(/AKIAIOSFODNN7EXAMPLE/);
  });
});
