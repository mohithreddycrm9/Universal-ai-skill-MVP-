import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { SkillTrustGateway } from "../gateway.js";
import { newId } from "../util/ids.js";
import { encodeEnvelope, encodeError } from "./envelope.js";

const MAX = 32_768;

function toolResult(payload: unknown) {
  return { content: [{ type: "text" as const, text: JSON.stringify(payload) }] };
}

export function createMcpServer(gateway: SkillTrustGateway): McpServer {
  const server = new McpServer({ name: "universal-skills-mcp", version: "0.3.0" });
  const max = gateway.config.registry.response.maxBytes ?? MAX;

  const wrap = async (fn: (requestId: string) => Promise<unknown> | unknown) => {
    const requestId = newId("req");
    try {
      const data = await fn(requestId);
      return toolResult(encodeEnvelope(requestId, data, max));
    } catch (error) {
      return { ...toolResult(encodeError(requestId, error)), isError: true };
    }
  };

  server.registerTool(
    "discover_skill",
    {
      title: "Discover skill",
      description: "Find capability candidates. Does not download a repository into context.",
      inputSchema: z.object({
        query: z.string().min(1).max(300),
        domain: z.string().max(80).optional(),
        limit: z.number().int().min(1).max(20).optional(),
      }),
    },
    async (args) => wrap((requestId) => gateway.discover({ ...args, requestId })),
  );

  server.registerTool(
    "search_skills",
    {
      title: "Search skills",
      description: "Search persisted gateway skills.",
      inputSchema: z.object({ query: z.string().min(1).max(300), limit: z.number().int().optional() }),
    },
    async (args) => wrap(() => gateway.searchSkills(args)),
  );

  server.registerTool(
    "get_skill",
    {
      title: "Get skill",
      description: "Progressive disclosure: level 0 metadata, 1 SKILL.md, 2 resource index, 3 one resource.",
      inputSchema: z.object({
        skillId: z.string(),
        level: z.number().int().min(0).max(3).optional(),
        resourcePath: z.string().max(240).optional(),
      }),
    },
    async (args) =>
      wrap((requestId) =>
        gateway.getSkill({
          skillId: args.skillId,
          level: (args.level ?? 0) as 0 | 1 | 2 | 3,
          resourcePath: args.resourcePath,
          requestId,
        }),
      ),
  );

  server.registerTool(
    "acquire_skill",
    {
      title: "Acquire skill",
      description: "Queue async acquisition. Does not block the user path on full scans unless wait=true.",
      inputSchema: z.object({
        query: z.string().max(300).optional(),
        candidateId: z.string().optional(),
        repositoryUrl: z.string().optional(),
        wait: z.boolean().optional(),
        approvalId: z.string().optional(),
      }),
    },
    async (args) => wrap((requestId) => gateway.acquire({ ...args, requestId })),
  );

  server.registerTool(
    "verify_skill",
    {
      title: "Verify skill",
      description: "Publisher provenance and commit pin. Does not imply a security pass.",
      inputSchema: z.object({ skillId: z.string() }),
    },
    async (args) => wrap((requestId) => gateway.verify({ skillId: args.skillId, requestId })),
  );

  server.registerTool(
    "scan_skill",
    {
      title: "Scan skill",
      description: "Enqueue configured scanners. Absence of a scanner is not PASS.",
      inputSchema: z.object({
        skillId: z.string(),
        wait: z.boolean().optional(),
      }),
    },
    async (args) => wrap((requestId) => gateway.scan({ ...args, requestId })),
  );

  server.registerTool(
    "get_skill_status",
    {
      title: "Skill status",
      description: "Lifecycle and job progress.",
      inputSchema: z.object({ skillId: z.string().optional(), jobId: z.string().optional() }),
    },
    async (args) => wrap(() => gateway.getSkillStatus(args)),
  );

  server.registerTool(
    "get_skill_security",
    {
      title: "Skill security",
      description: "Federated scanner summary. Not a universal safety claim.",
      inputSchema: z.object({ skillId: z.string() }),
    },
    async (args) => wrap((requestId) => gateway.getSkillSecurity({ skillId: args.skillId, requestId })),
  );

  server.registerTool(
    "get_skill_trust",
    {
      title: "Skill trust",
      description: "Trust evidence. Trust is not authorization.",
      inputSchema: z.object({ skillId: z.string() }),
    },
    async (args) => wrap((requestId) => gateway.getSkillTrust({ skillId: args.skillId, requestId })),
  );

  server.registerTool(
    "get_skill_permissions",
    {
      title: "Skill permissions",
      description: "Firewall-owned effective permissions.",
      inputSchema: z.object({ skillId: z.string() }),
    },
    async (args) => wrap(() => gateway.getSkillPermissions({ skillId: args.skillId })),
  );

  server.registerTool(
    "invalidate_skill",
    {
      title: "Invalidate skill",
      description: "Operator invalidation of a pinned skill version.",
      inputSchema: z.object({ skillId: z.string(), reason: z.string().min(1).max(500) }),
    },
    async (args) => wrap((requestId) => gateway.invalidate({ ...args, requestId })),
  );

  server.registerTool(
    "refresh_skill",
    {
      title: "Refresh skill",
      description: "Re-pin. A new commit is a new artifact (anti-rug-pull).",
      inputSchema: z.object({ skillId: z.string(), wait: z.boolean().optional() }),
    },
    async (args) => wrap((requestId) => gateway.refresh({ ...args, requestId })),
  );

  server.registerTool(
    "list_skills",
    {
      title: "List skills",
      description: "List registry skills.",
      inputSchema: z.object({
        lifecycle: z.string().optional(),
        limit: z.number().int().optional(),
        offset: z.number().int().optional(),
      }),
    },
    async (args) => wrap(() => gateway.listSkills(args)),
  );

  server.registerTool(
    "compare_skill_versions",
    {
      title: "Compare skill versions",
      description: "Summary diff of two pinned fingerprints.",
      inputSchema: z.object({ skillIdA: z.string(), skillIdB: z.string() }),
    },
    async (args) => wrap(() => gateway.compareVersions(args)),
  );

  server.registerTool(
    "request_capability",
    {
      title: "Request capability",
      description:
        "Ask the firewall for a capability. Creates a PENDING approval only — caller approver strings never authorize. After CLI `skill-mcp approve <id>`, re-invoke with approvalId.",
      inputSchema: z.object({
        skillId: z.string(),
        capability: z.string(),
        /** Ignored for authorization (claim only). */
        approver: z.string().optional(),
        approvalId: z.string().optional(),
      }),
    },
    async (args) => wrap((requestId) => gateway.requestCapability({ ...args, requestId })),
  );

  server.registerTool(
    "release_skill",
    {
      title: "Release skill",
      description: "Drop session materialization of quarantined bytes.",
      inputSchema: z.object({ skillId: z.string() }),
    },
    async (args) => wrap((requestId) => gateway.release({ skillId: args.skillId, requestId })),
  );

  server.registerTool(
    "list_integrations",
    {
      title: "List integrations",
      description: "Cost metadata for every source and scanner. Core is free/OSS-first.",
      inputSchema: z.object({}),
    },
    async () => wrap(() => gateway.listIntegrations()),
  );

  server.registerTool(
    "list_pending_cost_approvals",
    {
      title: "List pending cost approvals",
      description: "Human review queue for potentially billable operations.",
      inputSchema: z.object({}),
    },
    async () => wrap(() => gateway.listPendingCostApprovals()),
  );

  server.registerTool(
    "approve_paid_operation",
    {
      title: "Approve paid operation",
      description:
        "Does NOT authorize. MCP approver strings are untrusted. Use CLI: skill-mcp approve <id> (interactive y/N).",
      inputSchema: z.object({ approvalId: z.string(), approver: z.string().min(1).max(120).optional() }),
    },
    async (args) =>
      wrap((requestId) =>
        gateway.approvePaidOperation({ ...args, requestId, channel: "mcp" }),
      ),
  );

  server.registerTool(
    "reject_paid_operation",
    {
      title: "Reject paid operation",
      description:
        "Does NOT authorize reject via MCP. Use CLI: skill-mcp reject <id> (interactive y/N).",
      inputSchema: z.object({ approvalId: z.string(), approver: z.string().min(1).max(120).optional() }),
    },
    async (args) =>
      wrap((requestId) =>
        gateway.rejectPaidOperation({ ...args, requestId, channel: "mcp" }),
      ),
  );

  return server;
}

export const GATEWAY_TOOL_NAMES = [
  "discover_skill",
  "search_skills",
  "get_skill",
  "acquire_skill",
  "verify_skill",
  "scan_skill",
  "get_skill_status",
  "get_skill_security",
  "get_skill_trust",
  "get_skill_permissions",
  "invalidate_skill",
  "refresh_skill",
  "list_skills",
  "compare_skill_versions",
  "request_capability",
  "release_skill",
  "list_integrations",
  "list_pending_cost_approvals",
  "approve_paid_operation",
  "reject_paid_operation",
] as const;
