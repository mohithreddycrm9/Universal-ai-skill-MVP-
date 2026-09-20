import { describe, expect, it } from "vitest";
import { OssBinaryScanner } from "../../src/scanners/oss-binary.js";
import { StrixScanner } from "../../src/scanners/strix.js";
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
    const strix = await new StrixScanner(undefined, spawn).scan(target(), { binary: "strix", failOpen: false });
    expect(strix.status).toBe("ERROR");
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

  it("keeps STRIX INCONCLUSIVE when the CLI exits non-zero without a parsed report", async () => {
    const prevModel = process.env.STRIX_LLM;
    const prevKey = process.env.LLM_API_KEY;
    process.env.STRIX_LLM = "local/free-model";
    process.env.LLM_API_KEY = "local";
    try {
      const spawn: SpawnFn = (cmd, args) => {
        if (cmd === "docker") {
          return { status: 0, stdout: "Server Version", stderr: "" };
        }
        if (args[0] === "--version") {
          return { status: 0, stdout: "strix 0.1\n", stderr: "" };
        }
        expect(args[0]).toBe("--target");
        return { status: 2, stdout: "usage: strix", stderr: "" };
      };
      const run = await new StrixScanner(undefined, spawn).scan(target(), { enabled: true });
      expect(run.status).toBe("INCONCLUSIVE");
      expect(run.status).not.toBe("PASS");
    } finally {
      if (prevModel === undefined) delete process.env.STRIX_LLM;
      else process.env.STRIX_LLM = prevModel;
      if (prevKey === undefined) delete process.env.LLM_API_KEY;
      else process.env.LLM_API_KEY = prevKey;
    }
  });

  it("refuses strix cloud args", async () => {
    const spawn: SpawnFn = () => ({ status: 0, stdout: "strix 0.1\n", stderr: "" });
    const run = await new StrixScanner(undefined, spawn).scan(target(), {
      enabled: true,
      args: ["cloud", "pentest"],
    });
    expect(run.status).toBe("ERROR");
    expect(run.notes ?? "").toMatch(/refusing.*cloud/i);
  });

  it("errors when Docker is unavailable even if strix binary exists", async () => {
    const prevModel = process.env.STRIX_LLM;
    process.env.STRIX_LLM = "local/free-model";
    try {
      const spawn: SpawnFn = (cmd, args) => {
        if (args[0] === "--version") {
          return { status: 0, stdout: "strix 0.1\n", stderr: "" };
        }
        if (cmd === "docker") {
          return enoent();
        }
        return { status: 0, stdout: "", stderr: "" };
      };
      const run = await new StrixScanner(undefined, spawn).scan(target(), { enabled: true });
      expect(run.status).toBe("ERROR");
      expect(run.notes ?? "").toMatch(/docker/i);
    } finally {
      if (prevModel === undefined) delete process.env.STRIX_LLM;
      else process.env.STRIX_LLM = prevModel;
    }
  });
});
