import { describe, expect, it } from "vitest";
import { GitHubSource } from "../../src/discovery/github.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import { SkillMcpError } from "../../src/errors.js";

describe("GitHubSource", () => {
  it("restricts search to public repositories", async () => {
    const urls: string[] = [];
    const source = new GitHubSource("https://api.github.com", undefined, async (url) => {
      urls.push(url);
      return new Response(JSON.stringify({ items: [] }), { status: 200 });
    });
    await source.search({ query: "skill yaml", limit: 5 });
    expect(urls[0]).toContain("is%3Apublic");
    expect(source.costFor({ repositoryUrl: "https://github.com/acme/skill" })).toEqual(COST_CATALOG.github_public);
  });

  it("treats enterprise/private APIs as unknown-cost", () => {
    const source = new GitHubSource("https://github.acme.internal/api/v3", undefined, async () => new Response("{}", { status: 500 }));
    expect(source.costFor({ repositoryUrl: "https://github.acme.internal/org/repo" }).requiresApproval).toBe(true);
  });

  it("refuses to fetch a private repository without approval", async () => {
    const source = new GitHubSource("https://api.github.com", undefined, async (url) => {
      if (url.includes("/commits/")) {
        return new Response(JSON.stringify({ sha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" }), { status: 200 });
      }
      if (url.includes("/repos/acme/private")) {
        return new Response(
          JSON.stringify({
            owner: { login: "acme" },
            html_url: "https://github.com/acme/private",
            archived: false,
            private: true,
            created_at: "2020-01-01T00:00:00Z",
            license: null,
            default_branch: "main",
          }),
          { status: 200 },
        );
      }
      return new Response("{}", { status: 404 });
    });
    await expect(source.fetch({ repositoryUrl: "https://github.com/acme/private" })).rejects.toMatchObject({
      code: "COST_APPROVAL_REQUIRED",
    } satisfies Partial<SkillMcpError>);
  });
});
