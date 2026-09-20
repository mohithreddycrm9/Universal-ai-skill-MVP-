import { describe, expect, it } from "vitest";
import { customManifestToMcpSkill, parseSkillMd } from "../../src/skills/mcp-skill.js";
import { disclose } from "../../src/skills/disclosure.js";
import { parseManifest } from "../../src/skills/manifest.js";
import type { SkillRecord } from "../../src/types.js";

describe("MCP skill adapter + progressive disclosure", () => {
  it("parses SKILL.md frontmatter and adapts a custom manifest", () => {
    const parsed = parseSkillMd(`---
name: csv-normalize
description: Normalize CSV headers
---

Body instructions.
`);
    expect(parsed.name).toBe("csv-normalize");
    expect(parsed.body).toContain("Body instructions");
    const manifest = parseManifest({
      apiVersion: "skill.mcp/v1",
      kind: "Skill",
      metadata: {
        name: "legacy-name",
        description: "legacy",
        publisher: "fixture-org",
        repository: "https://example.local/fixture-org/csv",
        version: "1.0.0",
      },
      spec: {
        instructions: "legacy instructions",
        risk: "LOW",
        capabilitiesDeclared: ["filesystem.read"],
        entrypoints: [],
        files: [{ path: "SKILL.md", sha256: "sha256:x" }],
        dependencies: { lockHash: "sha256:lock" },
      },
    });
    const mcp = customManifestToMcpSkill(manifest, {
      sourceId: "local",
      publisher: "fixture-org",
      ownerLogin: "fixture-org",
      repository: "fixture-org/csv",
      repositoryUrl: "https://example.local/fixture-org/csv",
      commitSha: "aaa",
      version: "1.0.0",
      archived: false,
      files: [
        {
          path: "SKILL.md",
          content: `---
name: csv-normalize
description: Normalize CSV headers
---

Body instructions.
`,
        },
        { path: "ref.md", content: "reference" },
      ],
    });
    expect(mcp.name).toBe("csv-normalize");
    expect(mcp.resources[0]?.path).toBe("ref.md");
  });

  it("level 0 omits SKILL.md body", () => {
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
        instructions: "secret-ish body that must not appear at level 0",
        risk: "LOW",
        capabilitiesDeclared: ["filesystem.read"],
        entrypoints: [],
        files: [],
        dependencies: { lockHash: "sha256:lock" },
      },
    });
    const record = {
      id: "skl_1",
      name: "csv-normalize",
      publisher: "fixture-org",
      repository: "https://example.local/x",
      version: "1.0.0",
      commitSha: "aaa",
      fingerprint: "sha256:f",
      manifest,
      persistence: "TEMPORARY",
      lifecycle: "AVAILABLE",
      trustTier: "VERIFIED",
      securityStatus: "PASS",
      qualityStatus: "UNKNOWN",
      risk: "LOW",
      permissions: ["filesystem.read"],
      sbomSummary: null,
      sandboxSummary: null,
      expirationAt: null,
      createdAt: "",
      updatedAt: "",
    } as SkillRecord;
    const card0 = disclose(record, customManifestToMcpSkill(manifest), 0);
    expect(card0.skillMd).toBeUndefined();
    expect(JSON.stringify(card0).includes("secret-ish body")).toBe(false);
    const card1 = disclose(record, customManifestToMcpSkill(manifest), 1);
    expect(card1.skillMd).toContain("secret-ish body");
  });
});
