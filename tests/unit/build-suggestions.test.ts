import { describe, expect, it } from "vitest";
import {
  buildBlockedOnApprovals,
  computeBuildSuggestions,
  mergeGatewayEnrichment,
  skillMatchesGoal,
} from "../../src/agent/build-suggestions.js";
import { createMcpServer } from "../../src/mcp/server.js";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { testGateway, benignPackage } from "../helpers.js";

describe("build suggestions", () => {
  it("returns empty without build signals", () => {
    const result = computeBuildSuggestions({ agentState: "running", limit: 5 });
    expect(result.suggestions).toHaveLength(0);
    expect(result.emptyReason).toMatch(/goal/i);
  });

  it("ties suggestions to goal, command, and test failure", () => {
    const result = computeBuildSuggestions({
      agentState: "running",
      goal: "add CSV normalization endpoint",
      lastCommand: "npm test -- gateway",
      lastCommandExitCode: 1,
      recentEvents: [{ kind: "test_failed", summary: "gateway.test.ts" }],
      limit: 5,
    });
    expect(result.suggestions.length).toBeGreaterThan(0);
    const joined = result.suggestions.map((s) => `${s.label} ${s.prompt}`).join(" ");
    expect(joined).toMatch(/CSV normalization/i);
    expect(joined).toMatch(/gateway/i);
    expect(result.suggestions.every((s) => s.because)).toBe(true);
  });

  it("does not add approvals without build-linked events", () => {
    const base = computeBuildSuggestions({
      goal: "deploy API",
      recentEvents: [{ kind: "shell_pending" }],
      lastCommand: "npm run build",
    });
    const merged = mergeGatewayEnrichment(
      base,
      {
        pendingCostApprovalCount: 2,
        pendingCapabilityApprovalCount: 1,
        localSkillHints: [],
        discoveryHints: [],
      },
      8,
      { goal: "deploy API", recentEvents: [{ kind: "shell_pending" }], includeSkillHints: false },
    );
    expect(merged.suggestions.some((s) => s.label.includes("Approval"))).toBe(false);
  });

  it("adds approvals when build events reference gated work", () => {
    const base = computeBuildSuggestions({
      goal: "scan new skill",
      recentEvents: [{ kind: "scan_skill" }],
    });
    const merged = mergeGatewayEnrichment(
      base,
      {
        pendingCostApprovalCount: 1,
        pendingCapabilityApprovalCount: 0,
        localSkillHints: [],
        discoveryHints: [],
      },
      8,
      { goal: "scan new skill", recentEvents: [{ kind: "scan_skill" }], includeSkillHints: false },
    );
    expect(merged.suggestions.some((s) => s.because === "approval_pending")).toBe(true);
    expect(buildBlockedOnApprovals([{ kind: "scan_skill" }])).toBe(true);
  });

  it("matches skills to goal tokens only", () => {
    expect(skillMatchesGoal("csv normalization", "csv-normalize", "Normalize CSV headers")).toBe(true);
    expect(skillMatchesGoal("deploy kubernetes", "csv-normalize", "CSV only")).toBe(false);
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
        activeStep: "wire MCP handler",
        changedFiles: ["src/mcp/server.ts"],
        recentEvents: [{ kind: "shell_pending", summary: "npm test" }],
        lastCommand: "npm test",
        limit: 6,
        includeSkillHints: false,
      },
    });
    expect(result.isError).not.toBe(true);
    const text = (result.content as Array<{ text: string }>)[0]?.text ?? "";
    const envelope = JSON.parse(text) as {
      ok: boolean;
      data: { suggestions: Array<{ because?: string }>; buildContextUsed?: { goal?: string } };
    };
    expect(envelope.ok).toBe(true);
    expect(envelope.data.suggestions.length).toBeGreaterThan(0);
    expect(envelope.data.buildContextUsed?.goal).toContain("csv");
    await client.close();
    await server.close();
  });
});
