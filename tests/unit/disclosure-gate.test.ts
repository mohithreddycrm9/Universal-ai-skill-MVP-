import { describe, expect, it } from "vitest";
import { canDiscloseSkillContent, disclose } from "../../src/skills/disclosure.js";
import { customManifestToMcpSkill } from "../../src/skills/mcp-skill.js";
import { parseManifest } from "../../src/skills/manifest.js";
import type { SkillRecord } from "../../src/types.js";
import { SkillMcpError } from "../../src/errors.js";
import { benignPackage, testGateway } from "../helpers.js";

function record(overrides: Partial<SkillRecord> = {}): SkillRecord {
  const manifest = parseManifest({
    apiVersion: "skill.mcp/v1",
    kind: "Skill",
    metadata: {
      name: "csv-normalize",
      description: "Normalize CSV headers",
      publisher: "fixture-org",
      repository: "https://example.local/x",
      version: "1.0.0",
    },
    spec: {
      instructions: "secret body must stay gated",
      risk: "LOW",
      capabilitiesDeclared: ["filesystem.read"],
      entrypoints: [],
      files: [],
      dependencies: { lockHash: "sha256:lock" },
    },
  });
  return {
    id: "skl_1",
    name: "csv-normalize",
    publisher: "fixture-org",
    repository: "https://example.local/x",
    version: "1.0.0",
    commitSha: "aaa",
    fingerprint: "sha256:f",
    manifest,
    persistence: "TEMPORARY",
    lifecycle: "QUARANTINED",
    trustTier: "UNKNOWN",
    securityStatus: "NOT_RUN",
    qualityStatus: "UNKNOWN",
    risk: "LOW",
    permissions: ["filesystem.read"],
    sbomSummary: null,
    sandboxSummary: null,
    expirationAt: null,
    createdAt: "",
    updatedAt: "",
    ...overrides,
  } as SkillRecord;
}

describe("canDiscloseSkillContent", () => {
  it("allows level-0 metadata path conceptually for unverified skills (content gate false)", () => {
    const unverified = record();
    expect(canDiscloseSkillContent(unverified)).toBe(false);
    const card0 = disclose(unverified, customManifestToMcpSkill(unverified.manifest), 0);
    expect(card0.skillMd).toBeUndefined();
    expect(card0.contentBlocked).toBeUndefined();
    expect(JSON.stringify(card0)).not.toContain("secret body");
  });

  it("blocks level 1/2 content for unverified and blocked statuses", () => {
    const blockedStates: Array<Partial<SkillRecord>> = [
      { lifecycle: "QUARANTINED", securityStatus: "NOT_RUN", trustTier: "UNKNOWN" },
      { lifecycle: "REJECTED", securityStatus: "FAIL", trustTier: "UNTRUSTED" },
      { lifecycle: "AVAILABLE", securityStatus: "INCONCLUSIVE", trustTier: "VERIFIED" },
      { lifecycle: "AVAILABLE", securityStatus: "ERROR", trustTier: "VERIFIED" },
      { lifecycle: "AVAILABLE", securityStatus: "TIMEOUT", trustTier: "VERIFIED" },
      { lifecycle: "AVAILABLE", securityStatus: "FAIL", trustTier: "VERIFIED" },
      { lifecycle: "AVAILABLE", securityStatus: "PASS", trustTier: "UNKNOWN" },
      { lifecycle: "AVAILABLE", securityStatus: "PASS", trustTier: "UNTRUSTED" },
      { lifecycle: "APPROVED", securityStatus: "PASS", trustTier: "VERIFIED" },
    ];
    for (const overrides of blockedStates) {
      const skill = record(overrides);
      expect(canDiscloseSkillContent(skill)).toBe(false);
      const card1 = disclose(skill, customManifestToMcpSkill(skill.manifest), 1);
      expect(card1.skillMd).toBeUndefined();
      expect(card1.contentBlocked).toBe(true);
      const card2 = disclose(skill, customManifestToMcpSkill(skill.manifest), 2);
      expect(card2.resources).toBeUndefined();
      expect(card2.contentBlocked).toBe(true);
    }
  });

  it("allows verified AVAILABLE + PASS content at level 1", () => {
    const verified = record({
      lifecycle: "AVAILABLE",
      securityStatus: "PASS",
      trustTier: "VERIFIED",
    });
    expect(canDiscloseSkillContent(verified)).toBe(true);
    const card1 = disclose(verified, customManifestToMcpSkill(verified.manifest), 1);
    expect(card1.skillMd).toContain("secret body");
    expect(card1.contentBlocked).toBeUndefined();
  });

  it("get_skill throws POLICY_DENIED for level ≥ 1 on unverified skills", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: false,
      requestId: "disc-1",
    })) as { skillId: string };
    // Not waited — still QUARANTINED / early lifecycle
    expect(() =>
      gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "disc-1" }),
    ).toThrow(SkillMcpError);
    try {
      gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "disc-1" });
    } catch (error) {
      expect(error).toBeInstanceOf(SkillMcpError);
      expect((error as SkillMcpError).code).toBe("POLICY_DENIED");
    }
    const meta = gw.getSkill({ skillId: acquired.skillId, level: 0, requestId: "disc-1" }) as {
      skillMd?: string;
    };
    expect(meta.skillMd).toBeUndefined();
  });

  it("get_skill allows level 1 after verified AVAILABLE pipeline", async () => {
    const gw = testGateway([benignPackage()]);
    const acquired = (await gw.acquire({
      query: "csv-normalize",
      wait: true,
      requestId: "disc-2",
    })) as { skillId: string; lifecycle: string };
    expect(acquired.lifecycle).toBe("AVAILABLE");
    const card = gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "disc-2" }) as {
      skillMd?: string;
    };
    expect(card.skillMd).toBeTruthy();
  });
});
