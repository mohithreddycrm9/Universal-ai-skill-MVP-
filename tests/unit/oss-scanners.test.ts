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

const FREE_ONLY = {
  policy: "ALLOW_FREE_ONLY" as const,
  currency: "USD",
  allowUpToAmount: 0,
  preferFreeAlternatives: true,
  neverAutoPaidFallback: true,
  unknownCostRequiresApproval: true,
};

const ASK_BEFORE = { ...FREE_ONLY, policy: "ASK_BEFORE_ANY_PAID_OPERATION" as const };

function withEnv(vars: Record<string, string | undefined>, fn: () => Promise<void>): Promise<void> {
  const prev: Record<string, string | undefined> = {};
  for (const key of Object.keys(vars)) {
    prev[key] = process.env[key];
    const next = vars[key];
    if (next === undefined) delete process.env[key];
    else process.env[key] = next;
  }
  return fn().finally(() => {
    for (const key of Object.keys(vars)) {
      if (prev[key] === undefined) delete process.env[key];
      else process.env[key] = prev[key];
    }
  });
}

function healthyLocalSpawn(onTarget?: (args: readonly string[]) => SpawnResult): SpawnFn {
  return (cmd, args) => {
    if (cmd === "docker") {
      return { status: 0, stdout: "Server Version", stderr: "" };
    }
    if (args[0] === "--version") {
      return { status: 0, stdout: "strix 0.1\n", stderr: "" };
    }
    if (args[0] === "--target" || args.includes("--target")) {
      return onTarget ? onTarget(args) : { status: 0, stdout: "ok", stderr: "" };
    }
    if (args[0] === "cloud") {
      return { status: 0, stdout: "cloud", stderr: "" };
    }
    return { status: 0, stdout: "", stderr: "" };
  };
}

describe("STRIX LLM cost gate (ALLOW_FREE_ONLY)", () => {
  it("runs and spawns --target when model is proven local under ALLOW_FREE_ONLY", async () => {
    await withEnv({ STRIX_LLM: "ollama/llama3", LLM_API_KEY: undefined, OPENAI_API_KEY: undefined }, async () => {
      let targetSpawned = 0;
      const spawn = healthyLocalSpawn(() => {
        targetSpawned += 1;
        return { status: 0, stdout: "ok", stderr: "" };
      });
      const run = await new StrixScanner(undefined, spawn, FREE_ONLY).scan(target(), {
        enabled: true,
        costPolicy: FREE_ONLY,
      });
      expect(targetSpawned).toBe(1);
      expect(run.status).toBe("PASS");
    });
  });

  it("blocks external model under ALLOW_FREE_ONLY and does not spawn --target", async () => {
    await withEnv({ STRIX_LLM: "openai/gpt-4o", OPENAI_API_KEY: "sk-test", LLM_API_KEY: undefined }, async () => {
      let targetSpawned = 0;
      const spawn: SpawnFn = (cmd, args) => {
        if (cmd === "docker") return { status: 0, stdout: "ok", stderr: "" };
        if (args[0] === "--version") return { status: 0, stdout: "strix 0.1\n", stderr: "" };
        if (args[0] === "--target") {
          targetSpawned += 1;
          return { status: 0, stdout: "ok", stderr: "" };
        }
        return { status: 0, stdout: "", stderr: "" };
      };
      const run = await new StrixScanner(undefined, spawn, FREE_ONLY).scan(target(), {
        enabled: true,
        costPolicy: FREE_ONLY,
      });
      expect(targetSpawned).toBe(0);
      expect(run.status).toBe("NOT_RUN");
      expect(run.status).not.toBe("PASS");
      expect(run.notes ?? "").toMatch(/ALLOW_FREE_ONLY|no external LLM request/i);
      expect(run.notes ?? "").toMatch(/external|chargeable/i);
    });
  });

  it("blocks unknown provider under ALLOW_FREE_ONLY without --target spawn", async () => {
    await withEnv({ STRIX_LLM: "mystery-vendor/custom-model-xyz", LLM_API_KEY: undefined, OPENAI_API_KEY: undefined }, async () => {
      let targetSpawned = 0;
      const spawn: SpawnFn = (cmd, args) => {
        if (cmd === "docker") return { status: 0, stdout: "ok", stderr: "" };
        if (args[0] === "--version") return { status: 0, stdout: "strix 0.1\n", stderr: "" };
        if (args[0] === "--target") {
          targetSpawned += 1;
          return { status: 0, stdout: "ok", stderr: "" };
        }
        return { status: 0, stdout: "", stderr: "" };
      };
      const run = await new StrixScanner(undefined, spawn, FREE_ONLY).scan(target(), {
        enabled: true,
        costPolicy: FREE_ONLY,
      });
      expect(targetSpawned).toBe(0);
      expect(["NOT_RUN", "INCONCLUSIVE"]).toContain(run.status);
      expect(run.status).not.toBe("PASS");
      expect(run.notes ?? "").toMatch(/unknown|not proven|ALLOW_FREE_ONLY|no external LLM request/i);
    });
  });

  it("blocks when only an external API key is set under ALLOW_FREE_ONLY", async () => {
    await withEnv({ STRIX_LLM: undefined, LLM_MODEL: undefined, OPENAI_API_KEY: "sk-live-key", LLM_API_KEY: undefined }, async () => {
      let targetSpawned = 0;
      const spawn: SpawnFn = (cmd, args) => {
        if (cmd === "docker") return { status: 0, stdout: "ok", stderr: "" };
        if (args[0] === "--version") return { status: 0, stdout: "strix 0.1\n", stderr: "" };
        if (args[0] === "--target") {
          targetSpawned += 1;
          return { status: 0, stdout: "ok", stderr: "" };
        }
        return { status: 0, stdout: "", stderr: "" };
      };
      const run = await new StrixScanner(undefined, spawn, FREE_ONLY).scan(target(), {
        enabled: true,
        costPolicy: FREE_ONLY,
      });
      expect(targetSpawned).toBe(0);
      expect(run.status).toBe("NOT_RUN");
      expect(run.status).not.toBe("PASS");
      expect(run.notes ?? "").toMatch(/API key|external|ALLOW_FREE_ONLY|no external LLM request/i);
    });
  });

  it("allows external LLM under ASK_BEFORE only with explicit approval", async () => {
    await withEnv({ STRIX_LLM: "anthropic/claude-3-5-sonnet", LLM_API_KEY: "key", OPENAI_API_KEY: undefined }, async () => {
      let targetSpawned = 0;
      const spawn = healthyLocalSpawn(() => {
        targetSpawned += 1;
        return { status: 0, stdout: "ok", stderr: "" };
      });

      const blocked = await new StrixScanner(undefined, spawn, ASK_BEFORE).scan(target(), {
        enabled: true,
        costPolicy: ASK_BEFORE,
      });
      expect(targetSpawned).toBe(0);
      expect(blocked.status).toBe("NOT_RUN");
      expect(blocked.notes ?? "").toMatch(/approval|ASK_BEFORE|no external LLM request/i);

      const allowed = await new StrixScanner(undefined, spawn, ASK_BEFORE).scan(target(), {
        enabled: true,
        costPolicy: ASK_BEFORE,
        costApprovalStatus: "APPROVED",
        costApprovalId: "cst_test_approval",
      });
      expect(targetSpawned).toBe(1);
      expect(allowed.status).toBe("PASS");
    });
  });

  it("blocks mystery model even when SKILL_MCP_STRIX_LLM_IS_FREE attests under ALLOW_FREE_ONLY", async () => {
    await withEnv(
      { STRIX_LLM: "custom-hosted/whatever", SKILL_MCP_STRIX_LLM_IS_FREE: "1", OPENAI_API_KEY: undefined },
      async () => {
        let targetSpawned = 0;
        const spawn = healthyLocalSpawn(() => {
          targetSpawned += 1;
          return { status: 0, stdout: "ok", stderr: "" };
        });
        const run = await new StrixScanner(undefined, spawn, FREE_ONLY).scan(target(), {
          enabled: true,
          costPolicy: FREE_ONLY,
        });
        // Attestation alone cannot bypass ALLOW_FREE_ONLY for unrecognized models.
        expect(targetSpawned).toBe(0);
        expect(run.status).toBe("NOT_RUN");
        expect(run.notes ?? "").toMatch(/attest|local|ALLOW_FREE_ONLY|not proven|unknown/i);
      },
    );
  });

  it("allows ollama when SKILL_MCP_STRIX_LLM_IS_FREE attests under ALLOW_FREE_ONLY", async () => {
    await withEnv(
      { STRIX_LLM: "ollama/llama3", SKILL_MCP_STRIX_LLM_IS_FREE: "1", OPENAI_API_KEY: undefined },
      async () => {
        let targetSpawned = 0;
        const spawn = healthyLocalSpawn(() => {
          targetSpawned += 1;
          return { status: 0, stdout: "ok", stderr: "" };
        });
        const run = await new StrixScanner(undefined, spawn, FREE_ONLY).scan(target(), {
          enabled: true,
          costPolicy: FREE_ONLY,
        });
        expect(targetSpawned).toBe(1);
        expect(run.status).toBe("PASS");
      },
    );
  });
});
