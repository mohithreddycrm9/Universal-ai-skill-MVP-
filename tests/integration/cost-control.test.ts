import { describe, expect, it } from "vitest";
import { testConfig, testGateway, benignPackage, secretPackage } from "../helpers.js";
import type { SkillPackage } from "../../src/types.js";
import type { SkillSource } from "../../src/discovery/skill-source.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import type { CostMetadata } from "../../src/cost/types.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import type { SecurityScanner } from "../../src/scanners/types.js";
import { SecretScanner } from "../../src/scanners/secret.js";
import { SecurityOrchestrator } from "../../src/security/orchestrator.js";
import { InProcessSandbox } from "../../src/sandbox/in-process.js";

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

function askBeforeConfig() {
  return testConfig((cfg) => {
    cfg.cost.policy = "ASK_BEFORE_ANY_PAID_OPERATION";
  });
}

describe("cost control integration", () => {
  it("lets a free local acquire proceed under $0 free-only default", async () => {
    const gw = testGateway([benignPackage()]);
    const result = (await gw.acquire({ query: "csv-normalize", wait: true, requestId: "free" })) as {
      lifecycle: string;
      status: string;
    };
    expect(result.lifecycle).toBe("AVAILABLE");
    expect(result.status).not.toBe("NEEDS_COST_APPROVAL");
  });

  it("denies paid acquire under ALLOW_FREE_ONLY without offering a paid path", async () => {
    const pkg = benignPackage();
    const paid = new PaidRegistrySource(pkg);
    const gw = createGateway({
      config: testConfig(),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: new LocalSource(),
      sources: [paid],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });
    await expect(
      gw.acquire({
        repositoryUrl: pkg.repositoryUrl,
        wait: true,
        requestId: "paid-free-only",
      }),
    ).rejects.toThrow(/ALLOW_FREE_ONLY|forbids paid/i);
    const listed = gw.listSkills({});
    expect((listed as { items: unknown[] }).items).toHaveLength(0);
  });

  it("does not start a paid acquire without approval when ask-before is enabled", async () => {
    const pkg = benignPackage();
    const paid = new PaidRegistrySource(pkg);
    const gw = createGateway({
      config: askBeforeConfig(),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: new LocalSource(),
      sources: [paid],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });
    const result = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: true,
      requestId: "paid",
    })) as { status: string; approvalId: string; review: { notice: string } };
    expect(result.status).toBe("NEEDS_COST_APPROVAL");
    expect(result.review.notice).toMatch(/not required to approve/i);
    const listed = gw.listSkills({});
    expect((listed as { items: unknown[] }).items).toHaveLength(0);
  });

  it("stops when the human rejects, and proceeds after explicit approval (ask-before)", async () => {
    const pkg = benignPackage();
    const paid = new PaidRegistrySource(pkg);
    const gw = createGateway({
      config: askBeforeConfig(),
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
      requestId: "r1",
    })) as { approvalId: string };
    const rejected = gw.rejectLocalInteractive({
      approvalId: pending.approvalId,
      actor: "human",
      requestId: "r2",
    }) as { approval: { status: string } };
    expect(rejected.approval.status).toBe("REJECTED");
    await expect(
      gw.acquire({
        repositoryUrl: pkg.repositoryUrl,
        wait: true,
        approvalId: pending.approvalId,
        requestId: "r3",
      }),
    ).rejects.toThrow(/rejected/i);

    const again = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: false,
      requestId: "r4",
    })) as { approvalId: string };
    gw.approveLocalInteractive({ approvalId: again.approvalId, actor: "human", requestId: "r5" });
    const acquired = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: true,
      approvalId: again.approvalId,
      requestId: "r6",
    })) as { status: string; lifecycle?: string };
    expect(acquired.status).not.toBe("NEEDS_COST_APPROVAL");
  });

  it("never auto-runs a paid scanner when a free scanner fails", async () => {
    const paidStub: SecurityScanner = {
      id: "vendor_paid_stub",
      version: "1",
      cost: COST_CATALOG.cloud_sandbox,
      scan: async () => ({
        scannerId: "vendor_paid_stub",
        scannerVersion: "1",
        status: "PASS",
        findings: [],
        startedAt: "",
        finishedAt: "",
      }),
    };
    const config = testConfig((cfg) => {
      cfg.scanners.scanners.vendor_paid_stub = { enabled: true };
    });
    const orchestrator = new SecurityOrchestrator(
      [new SecretScanner(), paidStub],
      config,
      { now: () => new Date("2020-01-01T00:00:00.000Z") },
    );
    const result = await orchestrator.scan(
      { skillId: "skl_x", package: secretPackage(), quarantinePath: "/tmp/q" },
      "LOW",
    );
    const paid = result.scanners.find((run) => run.scannerId === "vendor_paid_stub");
    expect(paid?.status).toBe("NOT_RUN");
    expect(paid?.notes ?? "").toMatch(/not an automatic paid fallback/i);
    expect(paid?.status).not.toBe("PASS");
    const secret = result.scanners.find((run) => run.scannerId === "secret");
    expect(secret?.status).toBe("FAIL");
  });
});
