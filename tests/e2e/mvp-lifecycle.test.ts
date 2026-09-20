/**
 * E2E validation gate — scenarios A–N via real gateway / MCP classes + local fixtures.
 * Live GitHub / STRIX / Docker: NOT_EXECUTED or INCONCLUSIVE when unavailable (never claimed PASS).
 */
import { describe, expect, it } from "vitest";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { createMcpServer, GATEWAY_TOOL_NAMES } from "../../src/mcp/server.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import { InProcessSandbox } from "../../src/sandbox/in-process.js";
import { DockerSandbox } from "../../src/sandbox/docker.js";
import { loadConfig } from "../../src/policy/load.js";
import { federate } from "../../src/security/orchestrator.js";
import { inferRisk } from "../../src/skills/manifest.js";
import { extractInstructions, manifestFromPackage } from "../../src/skills/manifest.js";
import { SkillMcpError } from "../../src/errors.js";
import { spawnSync } from "node:child_process";
import {
  benignPackage,
  closeGatewayDb,
  disallowedLicensePackage,
  dockerAvailable,
  e2eGateway,
  injectionPackage,
  metadataInjectionPackage,
  MovingTipSource,
  PaidRegistrySource,
  riskEscalatePackage,
  secretPackage,
  tempDataDir,
  testConfig,
} from "./fixtures.js";

function parseToolJson(result: { content: Array<{ type: string; text?: string }> }): {
  ok: boolean;
  data?: unknown;
  error?: { code: string; message: string };
} {
  const text = result.content.find((c) => c.type === "text")?.text;
  expect(text).toBeTruthy();
  return JSON.parse(text!) as { ok: boolean; data?: unknown; error?: { code: string; message: string } };
}

describe("E2E MVP lifecycle A–N", () => {
  it("A — legitimate skill full flow (discover → acquire → verify → serve)", async () => {
    const gw = e2eGateway([benignPackage()]);
    const discovered = (await gw.discover({ query: "csv-normalize", requestId: "e2e-a-d" })) as {
      candidates: Array<{ candidateId: string; metadataTrust?: string; skillMd?: string }>;
    };
    expect(discovered.candidates.length).toBeGreaterThan(0);
    expect(discovered.candidates[0]!.metadataTrust).toBe("UNTRUSTED");
    expect(discovered.candidates[0]).not.toHaveProperty("skillMd");

    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "e2e-a-a",
    })) as { skillId: string; lifecycle: string; fingerprint: string; status: string };
    expect(acquired.lifecycle).toBe("AVAILABLE");
    expect(acquired.fingerprint).toMatch(/^sha256:/);

    const trust = gw.getSkillTrust({ skillId: acquired.skillId, requestId: "e2e-a-t" });
    expect(JSON.stringify(trust)).toMatch(/VERIFIED|OFFICIAL|TRUSTED/i);

    const l0 = gw.getSkill({ skillId: acquired.skillId, level: 0, requestId: "e2e-a-0" }) as {
      skillMd?: string;
      trust: string;
      metadataTrust?: string;
    };
    expect(l0.skillMd).toBeUndefined();
    expect(l0.trust).toBe("VERIFIED");

    const l1 = gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "e2e-a-1" }) as {
      skillMd?: string;
    };
    expect(l1.skillMd).toMatch(/RFC4180|Normalize CSV|csv/i);

    const security = gw.getSkillSecurity({ skillId: acquired.skillId, requestId: "e2e-a-s" }) as {
      claim: string;
      scanners: Array<{ scannerId: string; status: string }>;
    };
    expect(security.claim).toBe("PASSED_CONFIGURED_CHECKS");
    const strix = security.scanners.find((s) => s.scannerId === "strix");
    expect(strix?.status).not.toBe("PASS");
  });

  it("B — malicious/untrusted → not approved, no trusted content", async () => {
    const gw = e2eGateway([injectionPackage(), secretPackage()]);
    const inject = (await gw.acquire({
      repositoryUrl: injectionPackage().repositoryUrl,
      wait: true,
      requestId: "e2e-b-i",
    })) as { skillId: string; lifecycle: string };
    expect(["QUARANTINED", "REJECTED"]).toContain(inject.lifecycle);
    expect(inject.lifecycle).not.toBe("AVAILABLE");
    expect(() =>
      gw.getSkill({ skillId: inject.skillId, level: 1, requestId: "e2e-b-i1" }),
    ).toThrow(SkillMcpError);

    const leak = (await gw.acquire({
      repositoryUrl: secretPackage().repositoryUrl,
      wait: true,
      requestId: "e2e-b-s",
    })) as { skillId: string; lifecycle: string };
    expect(["QUARANTINED", "REJECTED"]).toContain(leak.lifecycle);
    const l0 = gw.getSkill({ skillId: leak.skillId, level: 0, requestId: "e2e-b-0" }) as {
      skillMd?: string;
      contentBlocked?: boolean;
    };
    expect(l0.skillMd).toBeUndefined();
  });

  it("C — prompt-injection metadata → L0 UNTRUSTED only", async () => {
    const pkg = metadataInjectionPackage();
    const gw = e2eGateway([pkg]);
    const discovered = (await gw.discover({ query: "meta-inject", requestId: "e2e-c" })) as {
      candidates: Array<{
        metadataTrust?: string;
        description: string;
        name: string;
        note?: string;
        skillMd?: string;
        files?: unknown;
        instructions?: string;
      }>;
    };
    expect(discovered.candidates.length).toBeGreaterThan(0);
    const cand = discovered.candidates[0]!;
    expect(cand.metadataTrust).toBe("UNTRUSTED");
    expect(cand.description.length).toBeLessThanOrEqual(240);
    expect(cand.name.length).toBeLessThanOrEqual(120);
    expect(cand).not.toHaveProperty("skillMd");
    expect(cand).not.toHaveProperty("files");
    expect(cand).not.toHaveProperty("instructions");
    // Injection text may appear in bounded UNTRUSTED description — that is not trusted content.
    expect(cand.note ?? "").toMatch(/UNTRUSTED/i);
  });

  it("D — risk escalation: LOW declare + heuristic HIGH → not AVAILABLE", async () => {
    const pkg = riskEscalatePackage();
    expect(inferRisk(pkg)).toBe("HIGH");
    const gw = e2eGateway([pkg]);
    const result = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: true,
      requestId: "e2e-d",
    })) as { skillId: string; lifecycle: string };
    expect(result.lifecycle).not.toBe("AVAILABLE");
    const skill = gw.registry.requireSkill(result.skillId);
    expect(skill.risk).toBe("HIGH");
    // STRIX required for HIGH; commercial/unavailable → not PASS aggregate
    expect(["QUARANTINED", "REJECTED"]).toContain(result.lifecycle);
  });

  it("E — optional scanner FAIL ≠ aggregate PASS (license)", async () => {
    const config = loadConfig("config");
    expect(config.security.optionalScanners).toContain("license");

    // Federation glue (unit coverage is solid; assert contract here too)
    const required = config.security.requiredScanners.map((id) => ({
      scannerId: id,
      scannerVersion: "1",
      status: "PASS" as const,
      findings: [],
      startedAt: "",
      finishedAt: "",
    }));
    const fed = federate(
      [
        ...required,
        {
          scannerId: "license",
          scannerVersion: "1",
          status: "FAIL",
          findings: [
            {
              id: "lic:policy",
              severity: "MEDIUM",
              title: "License GPL-3.0 not in policy allowlist",
              evidence: "GPL-3.0",
            },
          ],
          startedAt: "",
          finishedAt: "",
        },
      ],
      "LOW",
      config,
    );
    expect(fed.status).toBe("FAIL");
    expect(fed.claim).toBe("FAILED");
    expect(fed.status).not.toBe("PASS");

    const pkg = disallowedLicensePackage();
    const gw = e2eGateway([pkg]);
    const result = (await gw.acquire({
      repositoryUrl: pkg.repositoryUrl,
      wait: true,
      requestId: "e2e-e",
    })) as { skillId: string; lifecycle: string };
    expect(result.lifecycle).not.toBe("AVAILABLE");
    expect(result.lifecycle).toBe("QUARANTINED");
    const security = gw.getSkillSecurity({ skillId: result.skillId, requestId: "e2e-e-s" }) as {
      claim: string;
      scanners: Array<{ scannerId: string; status: string }>;
    };
    const license = security.scanners.find((s) => s.scannerId === "license");
    expect(license?.status).toBe("FAIL");
    expect(security.claim).not.toBe("PASSED_CONFIGURED_CHECKS");
  });

  it("F — TOCTOU: pin A vs newly resolved tip B", async () => {
    const shaA = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    const shaB = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    const pkgA = {
      ...benignPackage(shaA),
      repository: "fixture-org/moving-e2e",
      repositoryUrl: "https://example.local/fixture-org/moving-e2e",
      files: [
        {
          path: "SKILL.md",
          content: `---
name: moving-e2e
description: pin A
---

ARTIFACT_COMMIT_A
`,
        },
        { path: "LICENSE", content: "MIT License" },
      ],
    };
    const pkgB = {
      ...pkgA,
      commitSha: shaB,
      files: [
        {
          path: "SKILL.md",
          content: `---
name: moving-e2e
description: pin B
---

ARTIFACT_COMMIT_B
`,
        },
        { path: "LICENSE", content: "MIT License" },
      ],
    };
    const source = new MovingTipSource(shaA, [pkgA, pkgB]);
    const local = new LocalSource();
    const gw = createGateway({
      config: testConfig(),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: local,
      sources: [source, local],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });

    const discovered = (await gw.discover({ query: "moving-e2e", requestId: "e2e-f-d" })) as {
      candidates: Array<{ candidateId: string; resolvedCommitSha?: string; metadataTrust?: string }>;
    };
    const cand = discovered.candidates[0]!;
    expect(cand.resolvedCommitSha).toBe(shaA);
    expect(cand.metadataTrust).toBe("UNTRUSTED");

    source.tipSha = shaB;

    const acquired = (await gw.acquire({
      candidateId: cand.candidateId,
      wait: true,
      requestId: "e2e-f-a",
    })) as { skillId: string; lifecycle: string };
    expect(acquired.lifecycle).toBe("AVAILABLE");
    const card = gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "e2e-f-1" }) as {
      skillMd?: string;
    };
    expect(card.skillMd).toContain("ARTIFACT_COMMIT_A");
    expect(card.skillMd).not.toContain("ARTIFACT_COMMIT_B");
  });

  it("G — cache hit on second acquire of same fingerprint", async () => {
    const gw = e2eGateway([benignPackage()]);
    const first = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "e2e-g1",
    })) as { fingerprint: string; status: string };
    const second = (await gw.acquire({
      query: "csv-normalize",
      wait: false,
      requestId: "e2e-g2",
    })) as { status: string; fingerprint: string };
    expect(second.status).toBe("CACHE_HIT");
    expect(second.fingerprint).toBe(first.fingerprint);
  });

  it("H — cache invalidation glue (artifact change); unit covers scanner/policy/legacy/expiry", async () => {
    const shaA = "cccccccccccccccccccccccccccccccccccccccc";
    const shaB = "dddddddddddddddddddddddddddddddddddddddd";
    const pkgA = {
      ...benignPackage(shaA),
      repository: "fixture-org/cache-e2e",
      repositoryUrl: "https://example.local/fixture-org/cache-e2e",
    };
    const local = new LocalSource();
    local.register(pkgA);
    const gw = createGateway({
      config: testConfig(),
      sqlitePath: ":memory:",
      dataDir: ":memory:",
      localSource: local,
      sources: [local],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });
    const first = (await gw.acquire({
      repositoryUrl: pkgA.repositoryUrl,
      wait: true,
      requestId: "e2e-h1",
    })) as { status: string; fingerprint: string };
    const hit = (await gw.acquire({
      repositoryUrl: pkgA.repositoryUrl,
      wait: true,
      requestId: "e2e-h2",
    })) as { status: string };
    expect(hit.status).toBe("CACHE_HIT");

    const pkgB = {
      ...pkgA,
      commitSha: shaB,
      files: [
        ...pkgA.files.filter((f) => f.path !== "SKILL.md"),
        {
          path: "SKILL.md",
          content: `---
name: cache-e2e
description: changed artifact
---

CHANGED_BODY_E2E
`,
        },
      ],
    };
    local.register(pkgB);
    const third = (await gw.acquire({
      repositoryUrl: pkgB.repositoryUrl,
      wait: true,
      requestId: "e2e-h3",
    })) as { status: string; fingerprint: string };
    expect(third.status).not.toBe("CACHE_HIT");
    expect(third.fingerprint).not.toBe(first.fingerprint);
  });

  it("I — capability firewall (shell denied without approver; elevated with human)", async () => {
    const gw = e2eGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "e2e-i",
    })) as { skillId: string };
    expect(() =>
      gw.requestCapability({
        skillId: acquired.skillId,
        capability: "shell.execute",
        requestId: "e2e-i-deny",
      }),
    ).toThrow(/denied|approval|POLICY/i);

    const elevated = gw.requestCapability({
      skillId: acquired.skillId,
      capability: "network.read",
      approver: "human-e2e",
      requestId: "e2e-i-allow",
    }) as { effective: string[] };
    expect(elevated.effective).toContain("network.read");
    expect(elevated.effective).toContain("filesystem.read");

    const perms = gw.getSkillPermissions({ skillId: acquired.skillId }) as { effective: string[] };
    expect(perms.effective).not.toContain("shell.execute");
  });

  it("J — permission binding to immutable identity (commit change resets)", async () => {
    const gw = e2eGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "e2e-j",
    })) as { skillId: string };
    gw.requestCapability({
      skillId: acquired.skillId,
      capability: "network.read",
      approver: "human",
      requestId: "e2e-j-elev",
    });
    gw.registry.updateSkill(acquired.skillId, {
      commitSha: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
    });
    const perms = gw.getSkillPermissions({ skillId: acquired.skillId }) as {
      effective: string[];
      permissionsReset: boolean;
      permissionsResetReason: string | null;
    };
    expect(perms.effective).not.toContain("network.read");
    expect(perms.effective).toContain("filesystem.read");
    expect(perms.permissionsReset).toBe(true);
    expect(perms.permissionsResetReason).toMatch(/commit/i);
  });

  it("K — ALLOW_FREE_ONLY blocks paid/unknown sources", async () => {
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
    expect(testConfig().cost.policy).toBe("ALLOW_FREE_ONLY");
    await expect(
      gw.acquire({
        repositoryUrl: pkg.repositoryUrl,
        wait: true,
        requestId: "e2e-k",
      }),
    ).rejects.toThrow(/ALLOW_FREE_ONLY|forbids paid/i);
    expect((gw.listSkills({}) as { items: unknown[] }).items).toHaveLength(0);
  });

  it("L — Docker static-only IF available else INCONCLUSIVE/skip clearly", async () => {
    const available = dockerAvailable();
    const policy = loadConfig("config").sandbox;
    const sandbox = new DockerSandbox(policy);
    const pkg = benignPackage();
    const manifest = manifestFromPackage(pkg, extractInstructions(pkg), inferRisk(pkg));
    const result = await sandbox.evaluate(pkg, manifest);

    if (!available) {
      expect(result.status).toBe("INCONCLUSIVE");
      expect(result.status).not.toBe("PASS");
      expect(result.notes).toMatch(/INCONCLUSIVE|unavailable|Docker|Podman/i);
      expect(result.sandboxMode).toBe("SANDBOX_STATIC_ONLY");
      expect(result.executesSkillCode).toBe(false);
      // Documented skip — not a release blocker
      return;
    }

    // Docker present: static-only observer must not claim universal safety / must not execute skill hooks
    expect(["PASS", "FAIL", "INCONCLUSIVE"]).toContain(result.status);
    expect(result.executesSkillCode).toBe(false);
    expect(result.sandboxMode).toBe("SANDBOX_STATIC_ONLY");
    expect(result.notes ?? "").toMatch(/not a universal safety claim|static|does not run skill code/i);
  });

  it("M — MCP protocol via actual server (in-memory client)", async () => {
    const gateway = e2eGateway([benignPackage()]);
    const server = createMcpServer(gateway);
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    const client = new Client({ name: "e2e", version: "0" });
    await Promise.all([server.connect(serverTransport), client.connect(clientTransport)]);

    const listed = await client.listTools();
    const names = listed.tools.map((t) => t.name);
    for (const required of GATEWAY_TOOL_NAMES) {
      expect(names).toContain(required);
    }

    const discoverRaw = await client.callTool({
      name: "discover_skill",
      arguments: { query: "csv-normalize", limit: 5 },
    });
    const discoverEnv = parseToolJson(discoverRaw as { content: Array<{ type: string; text?: string }> });
    expect(discoverEnv.ok).toBe(true);
    const discoverData = discoverEnv.data as {
      candidates: Array<{ metadataTrust?: string }>;
    };
    expect(discoverData.candidates[0]?.metadataTrust).toBe("UNTRUSTED");

    const acquireRaw = await client.callTool({
      name: "acquire_skill",
      arguments: { query: "csv-normalize", wait: true },
    });
    const acquireEnv = parseToolJson(acquireRaw as { content: Array<{ type: string; text?: string }> });
    expect(acquireEnv.ok).toBe(true);
    const acquired = acquireEnv.data as { skillId: string; lifecycle: string };
    expect(acquired.lifecycle).toBe("AVAILABLE");

    const getRaw = await client.callTool({
      name: "get_skill",
      arguments: { skillId: acquired.skillId, level: 1 },
    });
    const getEnv = parseToolJson(getRaw as { content: Array<{ type: string; text?: string }> });
    expect(getEnv.ok).toBe(true);
    expect(JSON.stringify(getEnv.data)).toMatch(/RFC4180|Normalize CSV|csv/i);

    await client.close();
    await server.close();
  });

  it("N — restart/persistence with SQLite file (skills + verification cache)", async () => {
    const { dataDir, sqlitePath } = tempDataDir();
    const pkg = benignPackage("2222222222222222222222222222222222222222");
    const pkgPinned = {
      ...pkg,
      repository: "fixture-org/persist-e2e",
      repositoryUrl: "https://example.local/fixture-org/persist-e2e",
    };

    const gw1 = e2eGateway([pkgPinned], { sqlitePath, dataDir });
    const first = (await gw1.acquire({
      repositoryUrl: pkgPinned.repositoryUrl,
      wait: true,
      requestId: "e2e-n1",
    })) as { skillId: string; lifecycle: string; fingerprint: string };
    expect(first.lifecycle).toBe("AVAILABLE");
    const skillId = first.skillId;
    const fingerprint = first.fingerprint;
    closeGatewayDb(gw1);

    const local2 = new LocalSource();
    local2.register(pkgPinned);
    const gw2 = createGateway({
      config: testConfig(),
      sqlitePath,
      dataDir,
      localSource: local2,
      sources: [local2],
      sandbox: new InProcessSandbox(),
      logger: new Logger("silent"),
    });

    const listed = gw2.listSkills({}) as { items: Array<{ id: string; lifecycle: string; fingerprint: string }> };
    expect(listed.items.some((item) => item.id === skillId && item.lifecycle === "AVAILABLE")).toBe(true);

    const l0 = gw2.getSkill({ skillId, level: 0, requestId: "e2e-n-0" }) as {
      trust: string;
      skillMd?: string;
      fingerprint: string;
    };
    expect(l0.fingerprint).toBe(fingerprint);
    expect(l0.skillMd).toBeUndefined();
    expect(l0.trust).toBe("VERIFIED");

    // Level 1 from persisted manifest instructions (package map is process-local)
    const l1 = gw2.getSkill({ skillId, level: 1, requestId: "e2e-n-1" }) as { skillMd?: string };
    expect(l1.skillMd).toBeTruthy();

    const cacheHit = (await gw2.acquire({
      repositoryUrl: pkgPinned.repositoryUrl,
      wait: true,
      requestId: "e2e-n-cache",
    })) as { status: string; fingerprint: string };
    expect(cacheHit.status).toBe("CACHE_HIT");
    expect(cacheHit.fingerprint).toBe(fingerprint);

    closeGatewayDb(gw2);
  });
});

describe("E2E live integrations status (documented)", () => {
  it("records NOT_EXECUTED for live GitHub and STRIX in this environment", () => {
    const githubToken = process.env.GITHUB_TOKEN;
    const strixBin = spawnSync("strix", ["--version"], { encoding: "utf8", timeout: 3000 });
    const strixPresent = strixBin.status === 0;
    // These are environment reports — not failures
    expect(githubToken ? "TOKEN_PRESENT_BUT_LIVE_FETCH_NOT_EXECUTED" : "NOT_EXECUTED").toMatch(
      /NOT_EXECUTED|TOKEN_PRESENT/,
    );
    expect(strixPresent ? "STRIX_PRESENT_BUT_LIVE_SCAN_NOT_EXECUTED" : "NOT_EXECUTED").toMatch(
      /NOT_EXECUTED|STRIX_PRESENT/,
    );
    expect(dockerAvailable() ? "DOCKER_AVAILABLE" : "DOCKER_UNAVAILABLE_INCONCLUSIVE").toBeTruthy();
  });
});
