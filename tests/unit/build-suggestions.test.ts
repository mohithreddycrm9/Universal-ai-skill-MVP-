import { describe, expect, it } from "vitest";
import {
  computeBuildSuggestions,
  mergeGatewayEnrichment,
} from "../../src/agent/build-suggestions.js";
import { createMcpServer } from "../../src/mcp/server.js";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { testGateway, benignPackage } from "../helpers.js";

describe("build suggestions", () => {
  it("prioritizes terminal and test failures", () => {
    const result = computeBuildSuggestions({
      agentState: "running",
      recentEvents: [{ kind: "test_failed", summary: "auth.test.ts" }],
      lastCommandExitCode: 1,
      limit: 5,
    });
    const labels = result.suggestions.map((s) => s.label);
    expect(labels[0]).toMatch(/test/i);
    expect(result.suggestions.every((s) => s.id.startsWith("sug_"))).toBe(true);
  });

  it("merges pending approvals from gateway enrichment", () => {
    const base = computeBuildSuggestions({ agentState: "running", limit: 8 });
    const merged = mergeGatewayEnrichment(base, {
      pendingCostApprovalCount: 2,
      pendingCapabilityApprovalCount: 1,
      localSkillHints: [],
      discoveryHints: [],
    }, 8);
    expect(merged.suggestions.some((s) => s.label.includes("pending approval"))).toBe(true);
  });

  it("exposes get_build_suggestions via MCP", async () => {
    const gateway = testGateway([benignPackage()]);
    const server = createMcpServer(gateway);
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    const client = new Client({ name: "test", version: "0" });
    await Promise.all([server.connect(serverTransport), client.connect(clientTransport)]);
    const result = await client.callTool({
      name: "get_build_suggestions",
      arguments: {
        agentState: "running",
        goal: "csv normalization",
        recentEvents: [{ kind: "shell_pending" }],
        limit: 6,
        includeSkillHints: false,
      },
    });
    expect(result.isError).not.toBe(true);
    const text = (result.content as Array<{ text: string }>)[0]?.text ?? "";
    const envelope = JSON.parse(text) as { ok: boolean; data: { suggestions: unknown[]; uiHint: string } };
    expect(envelope.ok).toBe(true);
    expect(envelope.data.suggestions.length).toBeGreaterThan(0);
    expect(envelope.data.uiHint).toContain("chips");
    await client.close();
    await server.close();
  });
});
