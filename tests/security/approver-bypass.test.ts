/**
 * Attack tests 1–12: MCP caller-supplied approver / booleans must not authorize.
 */
import { describe, expect, it } from "vitest";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { benignPackage, testConfig, testGateway } from "../helpers.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import { InProcessSandbox } from "../../src/sandbox/in-process.js";
import type { SkillPackage } from "../../src/types.js";
import type { SkillSource } from "../../src/discovery/skill-source.js";
import type { CostMetadata } from "../../src/cost/types.js";
import { SkillMcpError } from "../../src/errors.js";

class PaidRegistrySource implements SkillSource {
  readonly id = "paid_registry";
  readonly cost: CostMetadata = {
    provider: "ExampleCorp",
    service: "Commercial skill index",
    pricingModel: "paid",
    freeTier: false,
    estimatedCost: "unknown",
    requiresApproval: true,
    purpose: "Paid skill catalog search/fetch",
    freeAlternative: "LocalRegistrySource",
  };

  constructor(private readonly pkg: SkillPackage) {}

  async search() {
    return [
      {
        candidateId: "paid:skill",
        sourceId: this.id,
        name: this.pkg.repository,
        description: "paid",
        publisher: this.pkg.publisher,
        repository: this.pkg.repository,
        repositoryUrl: this.pkg.repositoryUrl,
        defaultRef: this.pkg.commitSha,
      },
    ];
  }

  async fetch() {
    return this.pkg;
  }

  async pin() {
    return { repositoryUrl: this.pkg.repositoryUrl, commitSha: this.pkg.commitSha, ref: this.pkg.commitSha };
  }
}

async function acquireSkill() {
  const gw = testGateway([benignPackage()]);
  const acquired = (await gw.acquire({
    query: "csv-normalize",
    wait: true,
    requestId: "atk-acq",
  })) as { skillId: string };
  return { gw, skillId: acquired.skillId };
}

describe("approver bypass attack suite", () => {
  it("1 — MCP approver:\"human\" creates PENDING only (no elevation)", async () => {
    const { gw, skillId } = await acquireSkill();
    const out = gw.requestCapability({
      skillId,
      capability: "shell.execute",
      approver: "human",
      requestId: "atk-1",
    }) as { status: string; approvalId: string; effective: string[] };
    expect(out.status).toBe("NEEDS_CAPABILITY_APPROVAL");
    expect(out.approvalId).toMatch(/^cap_/);
    expect(out.effective).not.toContain("shell.execute");
    const perms = gw.getSkillPermissions({ skillId }) as { effective: string[] };
    expect(perms.effective).not.toContain("shell.execute");
  });

  it("2 — MCP approver:\"admin\" / claimed boolean-like still PENDING", async () => {
    const { gw, skillId } = await acquireSkill();
    const out = gw.requestCapability({
      skillId,
      capability: "network.read",
      approver: "true",
      requestId: "atk-2",
    }) as { status: string; effective: string[] };
    expect(out.status).toBe("NEEDS_CAPABILITY_APPROVAL");
    expect(out.effective).not.toContain("network.read");
  });

  it("3 — MCP shell.execute without approval stays denied / pending", async () => {
    const { gw, skillId } = await acquireSkill();
    const out = gw.requestCapability({
      skillId,
      capability: "shell.execute",
      requestId: "atk-3",
    }) as { status: string; effective: string[] };
    expect(out.status).toBe("NEEDS_CAPABILITY_APPROVAL");
    expect(out.effective).toEqual(expect.arrayContaining(["filesystem.read"]));
    expect(out.effective).not.toContain("shell.execute");
  });

  it("4 — MCP approve_paid_operation with approver string denied", async () => {
    const pkg = benignPackage();
    const paid = new PaidRegistrySource(pkg);
    const gw = createGateway({
      config: testConfig((cfg) => {
        cfg.cost.policy = "ASK_BEFORE_ANY_PAID_OPERATION";
      }),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: new LocalSource(),
      sources: [paid],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });
    const pending = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: false,
      requestId: "atk-4a",
    })) as { approvalId: string };
    expect(() =>
      gw.approvePaidOperation({
        approvalId: pending.approvalId,
        approver: "human",
        requestId: "atk-4b",
        channel: "mcp",
      }),
    ).toThrow(/MCP approver|do not authorize|POLICY/i);
    const still = gw.registry.getCostApproval(pending.approvalId);
    expect(still?.status).toBe("PENDING");
    expect(still?.method).toBeNull();
  });

  it("5 — legitimate CLI local_interactive approve → grant", async () => {
    const { gw, skillId } = await acquireSkill();
    const pending = gw.requestCapability({
      skillId,
      capability: "network.read",
      requestId: "atk-5a",
    }) as { approvalId: string };
    gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-5b", actor: "operator" });
    const granted = gw.requestCapability({
      skillId,
      capability: "network.read",
      approvalId: pending.approvalId,
      requestId: "atk-5c",
    }) as { status: string; effective: string[] };
    expect(granted.status).toBe("GRANTED");
    expect(granted.effective).toContain("network.read");
    const audits = (gw.listAudit(50, skillId) as { events: Array<{ action: string; actor: string }> }).events;
    expect(audits.some((e) => e.action === "CAPABILITY_REQUESTED" && e.actor === "agent")).toBe(true);
    expect(audits.some((e) => e.action === "CAPABILITY_APPROVED" && e.actor === "human")).toBe(true);
  });

  it("6 — expiry denies (Clock)", async () => {
    let now = new Date("2026-01-01T00:00:00.000Z");
    const clock = { now: () => now };
    const local = new LocalSource();
    local.register(benignPackage());
    const gw2 = createGateway({
      config: testConfig(),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: local,
      sources: [local],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
      clock,
    });
    const acquired = (await gw2.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "atk-6a",
    })) as { skillId: string };
    const pending = gw2.requestCapability({
      skillId: acquired.skillId,
      capability: "network.read",
      requestId: "atk-6b",
    }) as { approvalId: string };
    gw2.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-6c" });
    now = new Date("2026-01-01T02:00:00.000Z"); // past 30m TTL
    expect(() =>
      gw2.requestCapability({
        skillId: acquired.skillId,
        capability: "network.read",
        approvalId: pending.approvalId,
        requestId: "atk-6d",
      }),
    ).toThrow(/expir/i);
  });

  it("7 — wrong fingerprint binding denied", async () => {
    const { gw, skillId } = await acquireSkill();
    const pending = gw.requestCapability({
      skillId,
      capability: "network.read",
      requestId: "atk-7a",
    }) as { approvalId: string };
    gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-7b" });
    gw.registry.updateSkill(skillId, { fingerprint: "sha256:attacker-swapped" });
    expect(() =>
      gw.requestCapability({
        skillId,
        capability: "network.read",
        approvalId: pending.approvalId,
        requestId: "atk-7c",
      }),
    ).toThrow(/binding mismatch/i);
  });

  it("8 — wrong capability denied", async () => {
    const { gw, skillId } = await acquireSkill();
    const pending = gw.requestCapability({
      skillId,
      capability: "network.read",
      requestId: "atk-8a",
    }) as { approvalId: string };
    gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-8b" });
    expect(() =>
      gw.requestCapability({
        skillId,
        capability: "shell.execute",
        approvalId: pending.approvalId,
        requestId: "atk-8c",
      }),
    ).toThrow(/binding mismatch/i);
  });

  it("9 — wrong skillId denied", async () => {
    const gw = testGateway([benignPackage(), { ...benignPackage("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"), repository: "fixture-org/other", repositoryUrl: "https://example.local/fixture-org/other" }]);
    const a = (await gw.acquire({ query: "csv-normalize", wait: true, requestId: "atk-9a" })) as {
      skillId: string;
    };
    const b = (await gw.acquire({
      repositoryUrl: "https://example.local/fixture-org/other",
      wait: true,
      requestId: "atk-9b",
    })) as { skillId: string };
    const pending = gw.requestCapability({
      skillId: a.skillId,
      capability: "network.read",
      requestId: "atk-9c",
    }) as { approvalId: string };
    gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-9d" });
    expect(() =>
      gw.requestCapability({
        skillId: b.skillId,
        capability: "network.read",
        approvalId: pending.approvalId,
        requestId: "atk-9e",
      }),
    ).toThrow(/binding mismatch/i);
  });

  it("10 — high-risk one-time consume denies reuse", async () => {
    const { gw, skillId } = await acquireSkill();
    const pending = gw.requestCapability({
      skillId,
      capability: "shell.execute",
      requestId: "atk-10a",
    }) as { approvalId: string };
    gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-10b" });
    const first = gw.requestCapability({
      skillId,
      capability: "shell.execute",
      approvalId: pending.approvalId,
      requestId: "atk-10c",
    }) as { status: string; effective: string[] };
    expect(first.status).toBe("GRANTED");
    expect(first.effective).toContain("shell.execute");
    expect(gw.registry.getCapabilityApproval(pending.approvalId)?.status).toBe("CONSUMED");
    expect(() =>
      gw.requestCapability({
        skillId,
        capability: "shell.execute",
        approvalId: pending.approvalId,
        requestId: "atk-10d",
      }),
    ).toThrow(/CONSUMED|cannot authorize/i);
  });

  it("11 — REJECTED denies", async () => {
    const { gw, skillId } = await acquireSkill();
    const pending = gw.requestCapability({
      skillId,
      capability: "network.read",
      requestId: "atk-11a",
    }) as { approvalId: string };
    gw.rejectLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-11b" });
    expect(() =>
      gw.requestCapability({
        skillId,
        capability: "network.read",
        approvalId: pending.approvalId,
        requestId: "atk-11c",
      }),
    ).toThrow(/REJECTED|cannot authorize/i);
  });

  it("12 — SQLite persistence across restart", async () => {
    const dir = mkdtempSync(join(tmpdir(), "skill-mcp-appr-"));
    const sqlitePath = join(dir, "skill-mcp.sqlite");
    try {
      const local1 = new LocalSource();
      local1.register(benignPackage());
      const gw1 = createGateway({
        config: testConfig(),
        sqlitePath,
        dataDir: dir,
        localSource: local1,
        sources: [local1],
        sandbox: new InProcessSandbox(),
        logger: new Logger("silent"),
      });
      const acquired = (await gw1.acquire({
        query: "csv-normalize",
        wait: true,
        requestId: "atk-12a",
      })) as { skillId: string };
      const pending = gw1.requestCapability({
        skillId: acquired.skillId,
        capability: "network.read",
        requestId: "atk-12b",
      }) as { approvalId: string };
      gw1.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-12c" });

      const local2 = new LocalSource();
      local2.register(benignPackage());
      const gw2 = createGateway({
        config: testConfig(),
        sqlitePath,
        dataDir: dir,
        localSource: local2,
        sources: [local2],
        sandbox: new InProcessSandbox(),
        logger: new Logger("silent"),
      });
      const stored = gw2.registry.getCapabilityApproval(pending.approvalId);
      expect(stored?.status).toBe("APPROVED");
      expect(stored?.method).toBe("local_interactive");
      const granted = gw2.requestCapability({
        skillId: acquired.skillId,
        capability: "network.read",
        approvalId: pending.approvalId,
        requestId: "atk-12d",
      }) as { status: string; effective: string[] };
      expect(granted.status).toBe("GRANTED");
      expect(granted.effective).toContain("network.read");
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  });

  it("paid ops: local_interactive approve then proceed under ASK_BEFORE", async () => {
    const pkg = benignPackage();
    const paid = new PaidRegistrySource(pkg);
    const gw = createGateway({
      config: testConfig((cfg) => {
        cfg.cost.policy = "ASK_BEFORE_ANY_PAID_OPERATION";
      }),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: new LocalSource(),
      sources: [paid],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });
    const pending = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: false,
      requestId: "atk-paid-a",
    })) as { approvalId: string };
    expect(() =>
      gw.approvePaidOperation({
        approvalId: pending.approvalId,
        approver: "human",
        requestId: "atk-paid-b",
      }),
    ).toThrow(SkillMcpError);
    gw.approveLocalInteractive({ approvalId: pending.approvalId, requestId: "atk-paid-c", actor: "human" });
    const acquired = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: true,
      approvalId: pending.approvalId,
      requestId: "atk-paid-d",
    })) as { status: string };
    expect(acquired.status).not.toBe("NEEDS_COST_APPROVAL");
  });
});
