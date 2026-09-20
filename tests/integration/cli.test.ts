import { describe, expect, it } from "vitest";
import { spawnSync } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

describe("CLI", () => {
  it("exposes costs and approvals commands", () => {
    const dataDir = mkdtempSync(join(tmpdir(), "skill-mcp-cli-"));
    const env = {
      ...process.env,
      SKILL_MCP_DATA_DIR: dataDir,
      SKILL_MCP_SANDBOX: "in-process",
      SKILL_MCP_LOG_LEVEL: "silent",
    };
    const tsx = join("node_modules", "tsx", "dist", "cli.mjs");
    const costs = spawnSync(process.execPath, ["--experimental-sqlite", tsx, "src/index.ts", "costs", "--json"], {
      encoding: "utf8",
      timeout: 20_000,
      env,
    });
    expect(costs.status).toBe(0);
    expect(costs.stdout).toMatch(/github_public/);
    expect(costs.stdout).toMatch(/neverAutoPaidFallback|ASK_BEFORE_ANY_PAID_OPERATION/);
    const approvals = spawnSync(process.execPath, ["--experimental-sqlite", tsx, "src/index.ts", "approvals", "--json"], {
      encoding: "utf8",
      timeout: 20_000,
      env,
    });
    expect(approvals.status).toBe(0);
    expect(approvals.stdout).toMatch(/"items"/);
  });
});
