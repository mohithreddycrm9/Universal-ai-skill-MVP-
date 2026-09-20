import { describe, expect, it } from "vitest";
import type { SkillCandidate, SkillPackage } from "../../src/types.js";
import type { DiscoveryQuery, PinnedRef, SkillRef, SkillSource } from "../../src/discovery/skill-source.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import { SkillMcpError } from "../../src/errors.js";
import { createGateway } from "../../src/gateway.js";
import { LocalSource } from "../../src/discovery/local-source.js";
import { Logger } from "../../src/observability/log.js";
import { InProcessSandbox } from "../../src/sandbox/in-process.js";
import { benignPackage, testConfig } from "../helpers.js";

class MockGitHubLikeSource implements SkillSource {
  readonly id: string;
  readonly cost = COST_CATALOG.github_public;
  private readonly packages: SkillPackage[];

  constructor(id: string, packages: SkillPackage[]) {
    this.id = id;
    this.packages = packages.map((pkg) => ({ ...pkg, sourceId: id }));
  }

  async search(query: DiscoveryQuery): Promise<SkillCandidate[]> {
    const q = query.query.toLowerCase();
    return this.packages
      .filter((pkg) => pkg.repository.toLowerCase().includes(q) || pkg.files.some((f) => f.content.toLowerCase().includes(q)))
      .slice(0, query.limit)
      .map((pkg) => {
        const [owner, ...rest] = pkg.repository.split("/");
        return {
          candidateId: `${this.id}:${pkg.repository}`,
          sourceId: this.id,
          name: rest.join("/") || pkg.repository,
          description: `${this.id} candidate for ${pkg.repository}`,
          publisher: pkg.publisher,
          repository: pkg.repository,
          repositoryUrl: pkg.repositoryUrl,
          defaultRef: pkg.commitSha,
          owner,
          repo: rest.join("/"),
          commit: pkg.commitSha,
          metadata: { mock: this.id },
        };
      });
  }

  async fetch(ref: SkillRef): Promise<SkillPackage> {
    const hit = this.packages.find(
      (pkg) =>
        pkg.repositoryUrl === ref.repositoryUrl ||
        (ref.owner && ref.repo && pkg.repository === `${ref.owner}/${ref.repo}`),
    );
    if (!hit) {
      throw new SkillMcpError("NOT_FOUND", `Mock source ${this.id} missing ${ref.repositoryUrl}`);
    }
    return { ...hit, sourceId: this.id };
  }

  async pin(ref: SkillRef): Promise<PinnedRef> {
    const pkg = await this.fetch(ref);
    return { repositoryUrl: pkg.repositoryUrl, commitSha: pkg.commitSha, ref: pkg.commitSha };
  }
}

function gatewayWithSources(sources: SkillSource[], localPackages: SkillPackage[] = []) {
  const local = new LocalSource();
  for (const pkg of localPackages) {
    local.register(pkg);
  }
  return createGateway({
    config: testConfig(),
    sqlitePath: ":memory:",
    dataDir: ":memory:",
    localSource: local,
    sources: [...sources, local],
    sandbox: new InProcessSandbox(),
    logger: new Logger("silent"),
  });
}

describe("discover → acquire by candidateId", () => {
  it("persists sourceId/owner/repo/ref and fetches from the original SkillSource", async () => {
    const pkgA = {
      ...benignPackage("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
      repository: "fixture-org/from-github-a",
      repositoryUrl: "https://example.local/fixture-org/from-github-a",
      files: [
        {
          path: "SKILL.md",
          content: `---
name: from-github-a
description: Package from mock github source A
---

Unique body marker SOURCE_A_ARTIFACT
`,
        },
        { path: "LICENSE", content: "MIT" },
      ],
    };
    const sourceA = new MockGitHubLikeSource("github-mock-a", [pkgA]);
    const gw = gatewayWithSources([sourceA]);

    const discovered = (await gw.discover({ query: "from-github-a", requestId: "acq-1" })) as {
      candidates: SkillCandidate[];
    };
    expect(discovered.candidates.length).toBeGreaterThan(0);
    const candidate = discovered.candidates.find((c) => c.sourceId === "github-mock-a");
    expect(candidate).toBeTruthy();
    expect(candidate!.sourceId).toBe("github-mock-a");
    expect(candidate!.repositoryUrl).toContain("from-github-a");
    expect(candidate!.owner).toBe("fixture-org");
    expect(candidate!.repo).toBe("from-github-a");
    expect(candidate!.commit ?? candidate!.defaultRef).toBeTruthy();
    expect(candidate!.metadata).toBeTruthy();

    const acquired = (await gw.acquire({
      candidateId: candidate!.candidateId,
      wait: true,
      requestId: "acq-1b",
    })) as { skillId: string; lifecycle: string };
    expect(acquired.lifecycle).toBe("AVAILABLE");
    const card = gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "acq-1c" }) as {
      skillMd?: string;
    };
    expect(card.skillMd).toContain("SOURCE_A_ARTIFACT");
  });

  it("does not silently fall back to LocalSource for a remote candidate", async () => {
    const remotePkg = {
      ...benignPackage("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
      repository: "fixture-org/remote-only",
      repositoryUrl: "https://example.local/fixture-org/remote-only",
      files: [
        {
          path: "SKILL.md",
          content: `---
name: remote-only
description: Remote candidate
---

REMOTE_SOURCE_BODY
`,
        },
      ],
    };
    // Local has a *different* package that would wrongly match a naive query fallback
    const localTrap = {
      ...benignPackage("cccccccccccccccccccccccccccccccccccccccc"),
      repository: "fixture-org/remote-only",
      repositoryUrl: "https://example.local/fixture-org/remote-only",
      files: [
        {
          path: "SKILL.md",
          content: `---
name: remote-only
description: Local trap
---

LOCAL_TRAP_BODY
`,
        },
      ],
    };
    const sourceA = new MockGitHubLikeSource("github-mock-a", [remotePkg]);
    const gw = gatewayWithSources([sourceA], [localTrap]);

    const discovered = (await gw.discover({ query: "remote-only", requestId: "acq-2" })) as {
      candidates: SkillCandidate[];
    };
    const remoteCandidate = discovered.candidates.find((c) => c.sourceId === "github-mock-a");
    expect(remoteCandidate).toBeTruthy();

    const acquired = (await gw.acquire({
      candidateId: remoteCandidate!.candidateId,
      wait: true,
      requestId: "acq-2b",
    })) as { skillId: string };
    const card = gw.getSkill({ skillId: acquired.skillId, level: 1, requestId: "acq-2c" }) as {
      skillMd?: string;
    };
    expect(card.skillMd).toContain("REMOTE_SOURCE_BODY");
    expect(card.skillMd).not.toContain("LOCAL_TRAP_BODY");
  });

  it("source A candidate does not resolve to source B artifact", async () => {
    const pkgA = {
      ...benignPackage("dddddddddddddddddddddddddddddddddddddddd"),
      repository: "fixture-org/shared-name",
      repositoryUrl: "https://example.local/a/shared-name",
      files: [
        {
          path: "SKILL.md",
          content: `---
name: shared-name
description: From A
---

ARTIFACT_FROM_SOURCE_A
`,
        },
      ],
    };
    const pkgB = {
      ...benignPackage("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"),
      repository: "fixture-org/shared-name",
      repositoryUrl: "https://example.local/b/shared-name",
      files: [
        {
          path: "SKILL.md",
          content: `---
name: shared-name
description: From B
---

ARTIFACT_FROM_SOURCE_B
`,
        },
      ],
    };
    const sourceA = new MockGitHubLikeSource("github-mock-a", [pkgA]);
    const sourceB = new MockGitHubLikeSource("github-mock-b", [pkgB]);
    const gw = gatewayWithSources([sourceA, sourceB]);

    const discovered = (await gw.discover({ query: "shared-name", requestId: "acq-3" })) as {
      candidates: SkillCandidate[];
    };
    const candA = discovered.candidates.find((c) => c.sourceId === "github-mock-a");
    const candB = discovered.candidates.find((c) => c.sourceId === "github-mock-b");
    expect(candA).toBeTruthy();
    expect(candB).toBeTruthy();
    expect(candA!.candidateId).not.toBe(candB!.candidateId);

    const acquiredA = (await gw.acquire({
      candidateId: candA!.candidateId,
      wait: true,
      requestId: "acq-3a",
    })) as { skillId: string };
    const cardA = gw.getSkill({ skillId: acquiredA.skillId, level: 1, requestId: "acq-3a2" }) as {
      skillMd?: string;
    };
    expect(cardA.skillMd).toContain("ARTIFACT_FROM_SOURCE_A");
    expect(cardA.skillMd).not.toContain("ARTIFACT_FROM_SOURCE_B");

    const acquiredB = (await gw.acquire({
      candidateId: candB!.candidateId,
      wait: true,
      requestId: "acq-3b",
    })) as { skillId: string };
    const cardB = gw.getSkill({ skillId: acquiredB.skillId, level: 1, requestId: "acq-3b2" }) as {
      skillMd?: string;
    };
    expect(cardB.skillMd).toContain("ARTIFACT_FROM_SOURCE_B");
    expect(cardB.skillMd).not.toContain("ARTIFACT_FROM_SOURCE_A");
  });

  it("unresolvable candidateId throws explicit SkillMcpError", async () => {
    const gw = gatewayWithSources([]);
    await expect(
      gw.acquire({ candidateId: "github:missing/repo", wait: false, requestId: "acq-4" }),
    ).rejects.toMatchObject({
      name: "SkillMcpError",
      code: "NOT_FOUND",
    });
  });

  it("missing SkillSource for a remembered candidate refuses LocalSource fallback", async () => {
    const pkg = {
      ...benignPackage("ffffffffffffffffffffffffffffffffffffffff"),
      repository: "fixture-org/orphan",
      repositoryUrl: "https://example.local/fixture-org/orphan",
    };
    const sourceA = new MockGitHubLikeSource("github-mock-a", [pkg]);
    const gw = gatewayWithSources([sourceA]);
    const discovered = (await gw.discover({ query: "orphan", requestId: "acq-5" })) as {
      candidates: SkillCandidate[];
    };
    const candidate = discovered.candidates.find((c) => c.sourceId === "github-mock-a");
    expect(candidate).toBeTruthy();

    // Simulate source disappearing after discover (e.g. reconfigured gateway without that source)
    (gw as unknown as { sources: SkillSource[] }).sources = [
      (gw as unknown as { localSource: LocalSource }).localSource,
    ];

    await expect(
      gw.acquire({ candidateId: candidate!.candidateId, wait: false, requestId: "acq-5b" }),
    ).rejects.toMatchObject({
      name: "SkillMcpError",
      code: "NOT_FOUND",
    });
  });
});
