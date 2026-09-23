import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { loadWorkspaceAsPackage } from "../../src/health/workspace.js";
import { CodeHealthScanner } from "../../src/scanners/code-health.js";
import type { ScanTarget } from "../../src/types.js";

describe("code health", () => {
  it("loads a workspace tree with ignores", () => {
    const root = mkdtempSync(join(tmpdir(), "health-"));
    writeFileSync(join(root, "package.json"), '{"name":"x"}');
    mkdirSync(join(root, "node_modules"), { recursive: true });
    writeFileSync(join(root, "node_modules", "ignored.js"), "eval('x')");
    writeFileSync(join(root, "app.ts"), "const x = 1;\n");

    const loaded = loadWorkspaceAsPackage(root);
    expect(loaded.filesScanned).toBe(2);
    expect(loaded.package.files.map((f) => f.path).sort()).toEqual(["app.ts", "package.json"]);
  });

  it("flags missing lockfile and eval", async () => {
    const root = mkdtempSync(join(tmpdir(), "health-"));
    writeFileSync(join(root, "package.json"), '{"name":"x"}');
    writeFileSync(join(root, "bad.ts"), "eval('1')");

    const loaded = loadWorkspaceAsPackage(root);
    const target: ScanTarget = {
      skillId: "h1",
      package: loaded.package,
      quarantinePath: root,
    };
    const run = await new CodeHealthScanner().scan(target, { enabled: true });
    expect(run.status).toBe("FAIL");
    expect(run.findings.some((f) => f.id === "health:missing-lockfile")).toBe(true);
    expect(run.findings.some((f) => f.id === "health:eval")).toBe(true);
  });
});
