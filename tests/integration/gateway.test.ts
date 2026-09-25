import { describe, expect, it } from "vitest";
import {
  benignPackage,
  injectionPackage,
  postinstallPackage,
  secretPackage,
  testConfig,
  testGateway,
} from "../helpers.js";

describe("gateway pipeline", () => {
  it("serves a verified fixture after configured checks, with level-0 cards", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "t1",
    })) as { skillId: string; lifecycle: string; fingerprint: string };
    expect(acquired.lifecycle).toBe("AVAILABLE");
    const card = gw.getSkill({ skillId: acquired.skillId, level: 0, requestId: "t1" }) as { skillMd?: string; trust: string };
    expect(card.skillMd).toBeUndefined();
    expect(card.trust).toBe("VERIFIED");
    const security = gw.getSkillSecurity({ skillId: acquired.skillId, requestId: "t1" }) as {
      claim: string;
      scanners: Array<{ scannerId: string; status: string }>;
    };
    expect(security.claim).toBe("PASSED_CONFIGURED_CHECKS");
  });

  it("hits the verification cache on a second acquire of the same fingerprint", async () => {
    const pkg = benignPackage();
    const gw = testGateway([pkg]);
    await gw.acquire({ query: "csv-normalize", wait: true, requestId: "t2a" });
    const second = (await gw.acquire({ query: "csv-normalize", wait: false, requestId: "t2b" })) as { status: string };
    expect(second.status).toBe("CACHE_HIT");
  });

  it("quarantines prompt injection and secret fixtures", async () => {
    const gw = testGateway([injectionPackage(), secretPackage()]);
    const inject = (await gw.acquire({
      repositoryUrl: injectionPackage().repositoryUrl,
      wait: true,
      requestId: "t3",
    })) as { lifecycle: string };
    expect(["QUARANTINED", "REJECTED"]).toContain(inject.lifecycle);
    const leak = (await gw.acquire({
      repositoryUrl: secretPackage().repositoryUrl,
      wait: true,
      requestId: "t4",
    })) as { lifecycle: string };
    expect(["QUARANTINED", "REJECTED"]).toContain(leak.lifecycle);
  });

  it("rejects install-hook skills (HIGH risk stays quarantined/rejected)", async () => {
    const gw = testGateway([postinstallPackage()]);
    const result = (await gw.acquire({
      repositoryUrl: postinstallPackage().repositoryUrl,
      wait: true,
      requestId: "t5",
    })) as { lifecycle: string };
    expect(result.lifecycle).not.toBe("AVAILABLE");
  });

  it("deduplicates concurrent acquisition jobs for one fingerprint", async () => {
    const gw = testGateway([benignPackage()]);
    const first = (await gw.acquire({ query: "csv", wait: false, requestId: "d1" })) as { jobId: string; status: string };
    const second = (await gw.acquire({ query: "csv", wait: false, requestId: "d2" })) as { status: string; jobId?: string };
    expect(first.status).toBe("ACCEPTED");
    expect(second.status).toBe("DEDUPLICATED");
    expect(second.jobId).toBe(first.jobId);
  });

  it("refuses mutable branch names as identity", async () => {
    const pkg = { ...benignPackage("main"), commitSha: "main" };
    const gw = testGateway([pkg]);
    await expect(gw.acquire({ query: "csv", wait: true, requestId: "mut" })).rejects.toThrow(/mutable/i);
  });

  it("keeps trust independent from authorization (shell still denied)", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({ query: "csv", wait: true, requestId: "authz" })) as { skillId: string };
    const pending = gw.requestCapability({
      skillId: acquired.skillId,
      capability: "shell.execute",
      approver: "human",
      requestId: "authz",
    }) as { status: string; effective: string[] };
    expect(pending.status).toBe("NEEDS_CAPABILITY_APPROVAL");
    expect(pending.effective).not.toContain("shell.execute");
    const perms = gw.getSkillPermissions({ skillId: acquired.skillId }) as { effective: string[] };
    expect(perms.effective).toContain("filesystem.read");
    expect(perms.effective).not.toContain("shell.execute");
  });

  it("does not auto-approve UNKNOWN publishers even if scans would pass", async () => {
    const pkg = { ...benignPackage(), publisher: "random-person", ownerLogin: "random-person" };
    const gw = testGateway([pkg], testConfig((cfg) => {
      cfg.trust.verifiedPublishers = [];
    }));
    const result = (await gw.acquire({ query: "csv", wait: true, requestId: "unk" })) as { lifecycle: string };
    expect(result.lifecycle).not.toBe("AVAILABLE");
  });
});
