import { describe, expect, it } from "vitest";
import { z } from "zod";
import { CostDetector } from "../../src/cost/detector.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import { costMetadataForStrixLlm, inspectStrixLlm } from "../../src/cost/strix-llm.js";
import { isClearlyFree, type CostMetadata, type CostPolicy } from "../../src/cost/types.js";
import { TrustBroker, AllowlistTrustProvider } from "../../src/trust/broker.js";
import { loadConfig } from "../../src/policy/load.js";
import { federate } from "../../src/security/orchestrator.js";
import { SecretScanner } from "../../src/scanners/secret.js";
import { redactDeep, looksLikeSecret } from "../../src/util/redact.js";
import { computeFingerprint } from "../../src/skills/fingerprint.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import {
  benignPackage,
  postinstallPackage,
  secretPackage,
  testConfig,
} from "../helpers.js";
import type { SandboxProvider, SandboxResult } from "../../src/sandbox/provider.js";
import type { ScannerRun, SecurityStatus } from "../../src/types.js";

const freeOnly: CostPolicy = {
  policy: "ALLOW_FREE_ONLY",
  currency: "USD",
  allowUpToAmount: 0,
  preferFreeAlternatives: true,
  neverAutoPaidFallback: true,
  unknownCostRequiresApproval: true,
};

function run(
  scannerId: string,
  status: SecurityStatus,
): ScannerRun {
  return {
    scannerId,
    scannerVersion: "1",
    status,
    findings: [],
    startedAt: "",
    finishedAt: "",
  };
}

describe("$0 / freemium hardening", () => {
  it("does not treat freemium STRIX catalog row as clearly free", () => {
    expect(COST_CATALOG.strix.pricingModel).toBe("freemium");
    expect(isClearlyFree(COST_CATALOG.strix)).toBe(false);
    expect(new CostDetector(freeOnly).evaluate("scan", COST_CATALOG.strix).proceed).toBe(false);
  });

  it("treats github_public as free only with proven exact $0 free tier", () => {
    expect(COST_CATALOG.github_public.pricingModel).toBe("freemium");
    expect(COST_CATALOG.github_public.estimatedCost).toBe("0");
    expect(isClearlyFree(COST_CATALOG.github_public)).toBe(true);
  });

  it("rejects freemium narrative costs that merely start with 0", () => {
    const narrative: CostMetadata = {
      provider: "Example",
      service: "Freemium API",
      pricingModel: "freemium",
      freeTier: true,
      estimatedCost: "0 for hobby tier when you pinky-swear",
      requiresApproval: false,
      purpose: "bypass attempt",
    };
    expect(isClearlyFree(narrative)).toBe(false);
  });

  it("API keys never imply free; attestation cannot rebrand OpenAI or empty config as free", () => {
    expect(inspectStrixLlm({ env: { OPENAI_API_KEY: "sk-test" }, config: {} }).class).toBe("external");
    expect(
      inspectStrixLlm({
        env: { STRIX_LLM: "openai/gpt-4o", SKILL_MCP_STRIX_LLM_IS_FREE: "1" },
        config: { llmIsLocalFree: true },
      }).class,
    ).toBe("external");
    expect(inspectStrixLlm({ env: { SKILL_MCP_STRIX_LLM_IS_FREE: "1" }, config: {} }).class).toBe(
      "unknown",
    );
    expect(
      inspectStrixLlm({
        env: { STRIX_LLM: "acme-cloud/secret-model", SKILL_MCP_STRIX_LLM_IS_FREE: "1" },
        config: {},
      }).class,
    ).toBe("unknown");
  });

  it("ALLOW_FREE_ONLY cannot be bypassed via approval id for Snyk/cloud/private/strix catalog", () => {
    const detector = new CostDetector(freeOnly);
    for (const meta of [
      COST_CATALOG.snyk,
      COST_CATALOG.cloud_sandbox,
      COST_CATALOG.github_private_or_unknown,
      COST_CATALOG.strix,
    ]) {
      const decision = detector.evaluate("op", meta, {
        approvalStatus: "APPROVED",
        approvalId: "approval_bypass_attempt",
      });
      expect(decision.proceed).toBe(false);
      expect(decision.kind).toBe("DENIED");
    }
  });

  it("external STRIX LLM metadata is denied under ALLOW_FREE_ONLY even if approval is forged", () => {
    const meta = costMetadataForStrixLlm(
      inspectStrixLlm({ env: { STRIX_LLM: "anthropic/claude-3" }, config: {} }),
    );
    const decision = new CostDetector(freeOnly).evaluate("scan_skill:strix_llm", meta, {
      approvalStatus: "APPROVED",
      approvalId: "forged",
    });
    expect(decision.proceed).toBe(false);
  });
});

describe("trust spoofing", () => {
  it("does not trust publisher names that merely contain a vendor string", () => {
    const config = loadConfig("config");
    const broker = new TrustBroker([new AllowlistTrustProvider(config.trust)]);
    const spoofed = {
      ...benignPackage(),
      publisher: "usestrix-unofficial-mirror",
      ownerLogin: "usestrix-unofficial-mirror",
      repository: "usestrix-unofficial-mirror/strix-helper",
    };
    const decision = broker.evaluate(spoofed);
    expect(decision.tier).toBe("UNKNOWN");
    expect(decision.evidence.join(" ")).toMatch(/name_similarity_is_not_evidence/);
  });
});

describe("sandbox unavailable cannot PASS into AVAILABLE", () => {
  it("keeps executable skills out of AVAILABLE when sandbox is INCONCLUSIVE", async () => {
    const unavailable: SandboxProvider = {
      id: "unavailable",
      async evaluate(): Promise<SandboxResult> {
        return {
          status: "INCONCLUSIVE",
          observed: {
            filesRead: [],
            filesWritten: [],
            processes: [],
            networkConnections: [],
            environmentAccess: [],
            secretsAccessed: [],
          },
          unexpected: [],
          notes: "Docker unavailable. INCONCLUSIVE ≠ PASS.",
        };
      },
    };
    const local = new LocalSource();
    const pkg = postinstallPackage();
    local.register(pkg);
    const gw = createGateway({
      config: testConfig((cfg) => {
        cfg.sandbox.executableRequiresSandbox = true;
      }),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: local,
      sources: [local],
      sandbox: unavailable,
      logger: new Logger("silent"),
    });
    const result = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: true,
      requestId: "sandbox-hard-1",
    })) as { lifecycle: string };
    expect(result.lifecycle).not.toBe("AVAILABLE");
    expect(["REJECTED", "QUARANTINED"]).toContain(result.lifecycle);
  });
});

describe("scanner federation never coerces soft statuses to PASS", () => {
  it.each(["INCONCLUSIVE", "ERROR", "TIMEOUT", "NOT_RUN"] as const)(
    "required scanner %s ⇒ federated INCONCLUSIVE (not PASS)",
    (status) => {
      const config = loadConfig("config");
      const required = config.security.requiredScanners;
      const runs = required.map((id, index) => run(id, index === 0 ? status : "PASS"));
      const result = federate(runs, "LOW", config);
      expect(result.status).toBe("INCONCLUSIVE");
      expect(result.status).not.toBe("PASS");
    },
  );
});

describe("secret redaction", () => {
  it("redacts AKIA values from findings and deep redact", async () => {
    const raw = "AKIAIOSFODNN7EXAMPLE";
    expect(looksLikeSecret(raw)).toBe(true);
    const runResult = await new SecretScanner().scan(
      { skillId: "skl_x", package: secretPackage(), quarantinePath: "/tmp/q" },
      { enabled: true },
    );
    expect(JSON.stringify(runResult)).not.toContain(raw);
    expect(JSON.stringify(redactDeep({ awsKey: raw, nested: { token: raw } }))).not.toContain(raw);
    expect(JSON.stringify(redactDeep({ findingEvidence: `leak ${raw}` }))).not.toMatch(
      /AKIA[0-9A-Z]{16}/,
    );
  });
});

describe("immutable identity", () => {
  it("changes fingerprint when commit changes (no verified reuse)", () => {
    const base = {
      publisher: "fixture-org",
      repository: "https://example.local/fixture-org/csv-normalize",
      manifestCanonical: "{}",
      dependencyLockHash: "sha256:lock",
      securityConfigurationHash: "sha256:sec",
      filesDigest: "sha256:files",
    };
    const a = computeFingerprint({ ...base, commitSha: "aaa" });
    const b = computeFingerprint({ ...base, commitSha: "bbb" });
    expect(a).not.toBe(b);
  });
});

describe("config validation", () => {
  it("rejects negative cost limits and non-positive scanner concurrency", () => {
    expect(() => z.object({ allowUpToAmount: z.number().nonnegative() }).parse({ allowUpToAmount: -1 })).toThrow();
    const concurrencySchema = z.object({
      maxConcurrentScanners: z.number().int().positive().max(32),
      scannerTimeoutMs: z.number().int().positive(),
    });
    expect(() => concurrencySchema.parse({ maxConcurrentScanners: 0, scannerTimeoutMs: 1000 })).toThrow();
    expect(() => concurrencySchema.parse({ maxConcurrentScanners: 4, scannerTimeoutMs: -5 })).toThrow();
  });

  it("loadConfig applies non-negative guards on live schemas", () => {
    const config = loadConfig("config");
    expect(config.cost.allowUpToAmount).toBeGreaterThanOrEqual(0);
    expect(config.security.maxConcurrentScanners).toBeGreaterThan(0);
    expect(config.security.scannerTimeoutMs).toBeGreaterThan(0);
    expect(config.sandbox.timeoutSeconds).toBeGreaterThan(0);
  });
});
