import { describe, expect, it } from "vitest";
import { computeFingerprint } from "../../src/skills/fingerprint.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import { InProcessSandbox } from "../../src/sandbox/in-process.js";
import { benignPackage, testConfig } from "../helpers.js";

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

  it("acquire cache hits same identity and misses after commit change", async () => {
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
