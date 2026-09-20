import { describe, expect, it } from "vitest";
import { computeFingerprint } from "../../src/skills/fingerprint.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import { InProcessSandbox } from "../../src/sandbox/in-process.js";
import { benignPackage, testConfig } from "../helpers.js";
import { securityConfigurationHash } from "../../src/policy/load.js";
import { VerificationCache } from "../../src/cache/verification-cache.js";
import {
  collectMaterialScannerImplementations,
  materialScannerVersionsRecord,
  normalizeScannerImplementationIdentities,
  type ScannerImplementationIdentity,
} from "../../src/cache/scanner-identity.js";
import { SkillRegistry } from "../../src/registry/skill-registry.js";
import { SqliteAdapter } from "../../src/registry/sqlite.js";
import type { Clock } from "../../src/util/clock.js";

function mutableClock(start = new Date("2026-01-01T00:00:00.000Z")): Clock & { advanceMs(ms: number): void } {
  let now = start.getTime();
  return {
    now: () => new Date(now),
    advanceMs(ms: number) {
      now += ms;
    },
  };
}

function registryWithClock(clock: Clock): SkillRegistry {
  return new SkillRegistry(new SqliteAdapter(":memory:"), clock);
}

const baseScanners = [
  { id: "secret", version: "1.0.0" },
  { id: "prompt_injection", version: "1.0.0" },
];

const enabledAll = {
  secret: { enabled: true },
  prompt_injection: { enabled: true },
};

describe("verification cache identity", () => {
  it("fingerprint (cache key) changes when commit or content or policy changes", () => {
    const base = {
      publisher: "fixture-org",
      repository: "https://example.local/fixture-org/csv-normalize",
      manifestCanonical: "{}",
      dependencyLockHash: "sha256:lock",
      securityConfigurationHash: "sha256:sec",
      filesDigest: "sha256:files-a",
    };
    const a = computeFingerprint({ ...base, commitSha: "aaa" });
    const b = computeFingerprint({ ...base, commitSha: "bbb" });
    const c = computeFingerprint({ ...base, commitSha: "aaa", filesDigest: "sha256:files-b" });
    const d = computeFingerprint({ ...base, commitSha: "aaa", securityConfigurationHash: "sha256:sec-2" });
    expect(a).not.toBe(b);
    expect(a).not.toBe(c);
    expect(a).not.toBe(d);
    expect(new Set([a, b, c, d]).size).toBe(4);
  });

  it("order A,B of material scanners equals B,A (deterministic sorted identity)", () => {
    const ab: ScannerImplementationIdentity[] = [
      { name: "secret", version: "1.0.0" },
      { name: "dependency", version: "2.0.0" },
    ];
    const ba: ScannerImplementationIdentity[] = [
      { name: "dependency", version: "2.0.0" },
      { name: "secret", version: "1.0.0" },
    ];
    expect(normalizeScannerImplementationIdentities(ab)).toEqual(
      normalizeScannerImplementationIdentities(ba),
    );
    const config = testConfig();
    expect(securityConfigurationHash(config, ab)).toBe(securityConfigurationHash(config, ba));
    expect(materialScannerVersionsRecord(ab)).toEqual(materialScannerVersionsRecord(ba));
  });

  it("same material scanner versions → same securityConfigurationHash (cache MAY hit)", () => {
    const config = testConfig();
    const material = collectMaterialScannerImplementations(baseScanners, enabledAll);
    const again = collectMaterialScannerImplementations(
      [
        { id: "prompt_injection", version: "1.0.0" },
        { id: "secret", version: "1.0.0" },
      ],
      enabledAll,
    );
    expect(securityConfigurationHash(config, material)).toBe(securityConfigurationHash(config, again));
  });

  it("scanner upgrade → securityConfigurationHash changes (miss)", () => {
    const config = testConfig();
    const v1 = collectMaterialScannerImplementations(baseScanners, enabledAll);
    const v2 = collectMaterialScannerImplementations(
      [
        { id: "secret", version: "1.1.0" },
        { id: "prompt_injection", version: "1.0.0" },
      ],
      enabledAll,
    );
    expect(securityConfigurationHash(config, v1)).not.toBe(securityConfigurationHash(config, v2));
  });

  it("scanner downgrade → securityConfigurationHash changes (miss)", () => {
    const config = testConfig();
    const v2 = collectMaterialScannerImplementations(
      [
        { id: "secret", version: "2.0.0" },
        { id: "prompt_injection", version: "1.0.0" },
      ],
      enabledAll,
    );
    const v1 = collectMaterialScannerImplementations(baseScanners, enabledAll);
    expect(securityConfigurationHash(config, v2)).not.toBe(securityConfigurationHash(config, v1));
  });

  it("add material scanner → securityConfigurationHash changes (miss)", () => {
    const config = testConfig();
    const before = collectMaterialScannerImplementations(baseScanners, enabledAll);
    const after = collectMaterialScannerImplementations(
      [...baseScanners, { id: "license", version: "1.0.0" }],
      { ...enabledAll, license: { enabled: true } },
    );
    expect(securityConfigurationHash(config, before)).not.toBe(securityConfigurationHash(config, after));
  });

  it("remove material scanner → securityConfigurationHash changes (miss)", () => {
    const config = testConfig();
    const withBoth = collectMaterialScannerImplementations(baseScanners, enabledAll);
    const withoutOne = collectMaterialScannerImplementations(baseScanners, {
      secret: { enabled: true },
      prompt_injection: { enabled: false },
    });
    expect(withBoth.map((s) => s.name).sort()).toEqual(["prompt_injection", "secret"]);
    expect(withoutOne.map((s) => s.name)).toEqual(["secret"]);
    expect(securityConfigurationHash(config, withBoth)).not.toBe(securityConfigurationHash(config, withoutOne));
  });

  it("policy change → securityConfigurationHash changes (miss)", () => {
    const material = collectMaterialScannerImplementations(baseScanners, enabledAll);
    const a = testConfig();
    const b = testConfig((c) => {
      c.security.failOnSeverity = ["CRITICAL"];
    });
    expect(securityConfigurationHash(a, material)).not.toBe(securityConfigurationHash(b, material));
  });

  it("VerificationCache: same versions hit; upgrade/downgrade/add/remove/unknown/legacy miss; expiry works", () => {
    const clock = mutableClock();
    const registry = registryWithClock(clock);
    const material: ScannerImplementationIdentity[] = [
      { name: "secret", version: "1.0.0" },
      { name: "dependency", version: "1.0.0" },
    ];
    const cache = new VerificationCache(registry, clock, { materialScanners: material });
    const fingerprint = "sha256:fp-same";
    const expiresAt = new Date(clock.now().getTime() + 60_000).toISOString();
    cache.put({
      fingerprint,
      skillId: "skl-1",
      securityStatus: "PASS",
      scannerVersions: materialScannerVersionsRecord(material),
      securityConfigHash: "sha256:cfg",
      expiresAt,
    });

    // same versions → hit
    expect(cache.get(fingerprint)?.skillId).toBe("skl-1");

    // upgrade → miss
    const upgraded = new VerificationCache(registry, clock, {
      materialScanners: [
        { name: "secret", version: "1.1.0" },
        { name: "dependency", version: "1.0.0" },
      ],
    });
    expect(upgraded.get(fingerprint)).toBeUndefined();

    // downgrade → miss
    const downgraded = new VerificationCache(registry, clock, {
      materialScanners: [
        { name: "secret", version: "0.9.0" },
        { name: "dependency", version: "1.0.0" },
      ],
    });
    expect(downgraded.get(fingerprint)).toBeUndefined();

    // add scanner → miss
    const added = new VerificationCache(registry, clock, {
      materialScanners: [
        ...material,
        { name: "license", version: "1.0.0" },
      ],
    });
    expect(added.get(fingerprint)).toBeUndefined();

    // remove material scanner → miss
    const removed = new VerificationCache(registry, clock, {
      materialScanners: [{ name: "secret", version: "1.0.0" }],
    });
    expect(removed.get(fingerprint)).toBeUndefined();

    // unknown version → deny reuse (fail closed)
    const unknown = new VerificationCache(registry, clock, {
      materialScanners: [
        { name: "secret", version: "unknown" },
        { name: "dependency", version: "1.0.0" },
      ],
    });
    expect(unknown.get(fingerprint)).toBeUndefined();

    const blank = new VerificationCache(registry, clock, {
      materialScanners: [
        { name: "secret", version: "  " },
        { name: "dependency", version: "1.0.0" },
      ],
    });
    expect(blank.get(fingerprint)).toBeUndefined();

    // legacy entry lacking scanner identity → miss
    registry.putCache({
      fingerprint: "sha256:fp-legacy",
      skillId: "skl-legacy",
      securityStatus: "PASS",
      scannerVersions: {},
      securityConfigHash: "sha256:cfg",
      createdAt: clock.now().toISOString(),
      expiresAt,
    });
    expect(cache.get("sha256:fp-legacy")).toBeUndefined();

    // expiry still works
    clock.advanceMs(120_000);
    expect(cache.get(fingerprint)).toBeUndefined();
  });

  it("different artifact → fingerprint miss (content/artifact identity preserved)", () => {
    const material = collectMaterialScannerImplementations(baseScanners, enabledAll);
    const config = testConfig();
    const sec = securityConfigurationHash(config, material);
    const base = {
      publisher: "fixture-org",
      repository: "https://example.local/fixture-org/csv-normalize",
      commitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      manifestCanonical: "{}",
      dependencyLockHash: "sha256:lock",
      securityConfigurationHash: sec,
    };
    const a = computeFingerprint({ ...base, filesDigest: "sha256:files-a" });
    const b = computeFingerprint({ ...base, filesDigest: "sha256:files-b" });
    expect(a).not.toBe(b);
  });

  it("acquire cache hits same identity and misses after commit change; ALLOW_FREE_ONLY preserved", async () => {
    const config = testConfig();
    expect(config.cost.policy).toBe("ALLOW_FREE_ONLY");
    expect(config.cost.allowUpToAmount).toBe(0);

    const shaA = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
    const shaB = "ffffffffffffffffffffffffffffffffffffffff";
    const pkgA = {
      ...benignPackage(shaA),
      repository: "fixture-org/cache-demo",
      repositoryUrl: "https://example.local/fixture-org/cache-demo",
    };
    const local = new LocalSource();
    local.register(pkgA);
    const gw = createGateway({
      config,
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
      requestId: "c1",
    })) as { status: string; fingerprint: string };
    expect(first.fingerprint).toBeTruthy();
    const second = (await gw.acquire({
      repositoryUrl: pkgA.repositoryUrl,
      wait: true,
      requestId: "c2",
    })) as { status: string; fingerprint: string };
    expect(second.status).toBe("CACHE_HIT");
    expect(second.fingerprint).toBe(first.fingerprint);

    const pkgB = {
      ...pkgA,
      commitSha: shaB,
      files: [
        ...pkgA.files.filter((f) => f.path !== "SKILL.md"),
        {
          path: "SKILL.md",
          content: `---
name: cache-demo
description: changed
---

CHANGED_BODY
`,
        },
      ],
    };
    local.register(pkgB);
    const third = (await gw.acquire({
      repositoryUrl: pkgB.repositoryUrl,
      wait: true,
      requestId: "c3",
    })) as { status: string; fingerprint: string };
    expect(third.status).not.toBe("CACHE_HIT");
    expect(third.fingerprint).not.toBe(first.fingerprint);
  });
});
