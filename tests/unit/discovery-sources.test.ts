import { describe, expect, it } from "vitest";
import { MCPRegistrySource } from "../../src/discovery/mcp-registry.js";
import { EnterpriseRegistrySource, OfficialVendorSource } from "../../src/discovery/allowlist-source.js";
import { sourcesFromRegistry } from "../../src/discovery/adapters.js";
import { loadConfig } from "../../src/policy/load.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import { isClearlyFree } from "../../src/cost/types.js";
import { SkillMcpError } from "../../src/errors.js";
import { readFileSync } from "node:fs";
import type { CatalogEntry } from "../../src/discovery/catalog.js";

const sample: CatalogEntry = {
  name: "csv-normalize",
  description: "Normalize CSV headers",
  publisher: "fixture-org",
  repository: "fixture-org/csv-normalize",
  repositoryUrl: "https://example.local/fixture-org/csv-normalize",
  defaultRef: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  commitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  license: "MIT",
  files: [{ path: "SKILL.md", content: "Normalize CSV headers. Do not call remote APIs." }],
};

describe("discovery adapters", () => {
  it("searches the MCP registry fixture catalog", async () => {
    const source = new MCPRegistrySource({ fixturePath: "config/mcp-registry.fixture.yaml" });
    expect(isClearlyFree(source.cost)).toBe(true);
    const hits = await source.search({ query: "markdown", limit: 5 });
    expect(hits.length).toBeGreaterThan(0);
    expect(hits[0]?.sourceId).toBe("mcp_registry");
    const pkg = await source.fetch({ repositoryUrl: hits[0]!.repositoryUrl });
    expect(pkg.commitSha).toMatch(/^[a-f0-9]{40}$/i);
    expect(pkg.files.some((file) => file.path === "SKILL.md")).toBe(true);
  });

  it("fail-closes unknown-cost remote MCP registry calls", async () => {
    const source = new MCPRegistrySource({
      mode: "remote",
      apiBase: "https://registry.example.invalid",
      remoteCost: COST_CATALOG.mcp_registry_remote,
    });
    expect(isClearlyFree(source.cost)).toBe(false);
    await expect(source.search({ query: "anything", limit: 3 })).rejects.toBeInstanceOf(SkillMcpError);
    try {
      await source.search({ query: "anything", limit: 3 });
    } catch (error) {
      expect(error).toBeInstanceOf(SkillMcpError);
      expect((error as SkillMcpError).code).toBe("COST_APPROVAL_REQUIRED");
    }
  });

  it("searches a free remote MCP registry when cost is clearly free", async () => {
    const source = new MCPRegistrySource({
      mode: "remote",
      apiBase: "https://registry.example.invalid",
      remoteCost: {
        provider: "example",
        service: "public catalog",
        pricingModel: "free",
        freeTier: true,
        estimatedCost: "0",
        requiresApproval: false,
        purpose: "test",
      },
      fetchImpl: async () =>
        new Response(
          JSON.stringify({
            servers: [
              {
                name: "example/docs",
                description: "How-to docs",
                repository: { url: "https://example.local/example/docs" },
              },
            ],
          }),
          { status: 200, headers: { "content-type": "application/json" } },
        ),
    });
    const hits = await source.search({ query: "docs", limit: 5 });
    expect(hits[0]?.repositoryUrl).toBe("https://example.local/example/docs");
  });

  it("loads official vendor and enterprise sources from allowlists (no vendor hard-coding)", async () => {
    const official = new OfficialVendorSource([sample]);
    const enterprise = new EnterpriseRegistrySource([]);
    expect(official.id).toBe("official_vendor");
    expect(enterprise.id).toBe("enterprise_registry");
    const hits = await official.search({ query: "csv", limit: 10 });
    expect(hits).toHaveLength(1);
    expect(await enterprise.search({ query: "csv", limit: 10 })).toEqual([]);
    const allowlistSrc = readFileSync("src/discovery/allowlist-source.ts", "utf8");
    const adaptersSrc = readFileSync("src/discovery/adapters.ts", "utf8");
    expect(`${allowlistSrc}\n${adaptersSrc}`).not.toMatch(/ServiceNow|salesforce/i);
  });

  it("wires enabled fixture sources from registry.yaml", () => {
    const config = loadConfig("config");
    const sources = sourcesFromRegistry(config.registry, config.configDir);
    expect(sources.map((item) => item.id)).toEqual(expect.arrayContaining(["mcp_registry", "official_vendor", "enterprise_registry"]));
    expect(sources.some((item) => item.id === "github")).toBe(false);
    expect(isClearlyFree(COST_CATALOG.mcp_registry_remote)).toBe(false);
  });
});
