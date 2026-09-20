import type { SkillCandidate, SkillPackage } from "../types.js";
import { SkillMcpError } from "../errors.js";
import { COST_CATALOG } from "../cost/catalog.js";
import type { CostMetadata } from "../cost/types.js";
import { isClearlyFree } from "../cost/types.js";
import type { DiscoveryQuery, PinnedRef, SkillRef, SkillSource } from "./skill-source.js";
import {
  type CatalogEntry,
  entryToCandidate,
  entryToPackage,
  findCatalogEntry,
  loadCatalogFile,
  matchCatalog,
} from "./catalog.js";
import type { FetchLike } from "./github.js";

export interface MCPRegistrySourceOptions {
  id?: string;
  mode?: "fixture" | "remote";
  fixturePath?: string;
  entries?: CatalogEntry[];
  apiBase?: string;
  fetchImpl?: FetchLike;
  remoteCost?: CostMetadata;
}

/**
 * MCP registry adapter. Default is a local/fixture catalog (free).
 * Remote calls are fail-closed unless the source is clearly free; unknown-cost
 * remotes require approval and never run as a silent fallback.
 */
export class MCPRegistrySource implements SkillSource {
  readonly id: string;
  readonly mode: "fixture" | "remote";
  readonly cost: CostMetadata;
  private readonly entries: CatalogEntry[];
  private readonly apiBase?: string;
  private readonly fetchImpl: FetchLike;
  private readonly remoteCost: CostMetadata;

  constructor(opts: MCPRegistrySourceOptions = {}) {
    this.id = opts.id ?? "mcp_registry";
    this.mode = opts.mode ?? "fixture";
    this.entries = opts.entries ?? (opts.fixturePath ? loadCatalogFile(opts.fixturePath) : []);
    this.apiBase = opts.apiBase?.replace(/\/+$/, "");
    this.fetchImpl = opts.fetchImpl ?? fetch;
    this.remoteCost = opts.remoteCost ?? COST_CATALOG.mcp_registry_remote;
    this.cost = this.mode === "remote" ? this.remoteCost : COST_CATALOG.mcp_registry_fixture;
  }

  costFor(): CostMetadata {
    return this.cost;
  }

  async search(query: DiscoveryQuery): Promise<SkillCandidate[]> {
    if (this.mode === "fixture") {
      return matchCatalog(this.entries, query.query, query.limit).map((entry) => entryToCandidate(entry, this.id));
    }
    this.assertRemoteAllowed();
    return this.searchRemote(query);
  }

  async fetch(ref: SkillRef): Promise<SkillPackage> {
    const local = findCatalogEntry(this.entries, ref.repositoryUrl, ref.owner && ref.repo ? `${ref.owner}/${ref.repo}` : ref.repo);
    if (local) {
      return entryToPackage(local, this.id);
    }
    if (this.mode === "remote") {
      this.assertRemoteAllowed();
      throw new SkillMcpError(
        "SOURCE_ERROR",
        "Remote MCP registry entries are catalog metadata only. Acquire a pinned local catalog entry or a public GitHub skill package. Refusing unbounded remote fetch.",
        { repositoryUrl: ref.repositoryUrl },
      );
    }
    throw new SkillMcpError("NOT_FOUND", `MCP registry fixture has no package for ${ref.repositoryUrl}`);
  }

  async pin(ref: SkillRef): Promise<PinnedRef> {
    const pkg = await this.fetch(ref);
    return { repositoryUrl: pkg.repositoryUrl, commitSha: pkg.commitSha, ref: pkg.commitSha };
  }

  private assertRemoteAllowed(): void {
    if (!this.apiBase) {
      throw new SkillMcpError(
        "SOURCE_ERROR",
        "MCP registry remote mode has no apiBase. Use the fixture catalog (free) or set a clearly-free apiBase.",
      );
    }
    if (!isClearlyFree(this.remoteCost)) {
      throw new SkillMcpError(
        "COST_APPROVAL_REQUIRED",
        "MCP registry remote API is not clearly free. Unknown/metered calls fail closed.",
        { metadata: this.remoteCost },
      );
    }
  }

  private async searchRemote(query: DiscoveryQuery): Promise<SkillCandidate[]> {
    const url = `${this.apiBase}/v0/servers?search=${encodeURIComponent(query.query)}&limit=${Math.min(query.limit, 20)}`;
    const response = await this.fetchImpl(url, {
      headers: { Accept: "application/json", "User-Agent": "universal-skill-trust-mcp" },
    });
    if (!response.ok) {
      throw new SkillMcpError("SOURCE_ERROR", `MCP registry request failed: ${response.status}`, { url });
    }
    const json = (await response.json()) as Record<string, unknown>;
    const rows = extractRemoteServers(json);
    return rows.slice(0, query.limit).map((row) => {
      const parts = row.repository.split("/").filter(Boolean);
      const owner = parts.length >= 2 ? parts[0] : row.publisher;
      const repo = parts.length >= 2 ? parts.slice(1).join("/") : parts[0];
      return {
        candidateId: `${this.id}:${row.repository}`,
        sourceId: this.id,
        name: row.name,
        description: row.description,
        publisher: row.publisher,
        repository: row.repository,
        repositoryUrl: row.repositoryUrl,
        defaultRef: row.defaultRef,
        owner,
        repo,
        metadata: { mcpRegistry: true },
      };
    });
  }
}

function extractRemoteServers(json: Record<string, unknown>): Array<{
  name: string;
  description: string;
  publisher: string;
  repository: string;
  repositoryUrl: string;
  defaultRef: string;
}> {
  const lists = [json.servers, json.items, json.results, json.data];
  const rows = lists.find((item) => Array.isArray(item)) as unknown[] | undefined;
  if (!rows) {
    return [];
  }
  const out: Array<{
    name: string;
    description: string;
    publisher: string;
    repository: string;
    repositoryUrl: string;
    defaultRef: string;
  }> = [];
  for (const row of rows) {
    if (!row || typeof row !== "object") {
      continue;
    }
    const rec = row as Record<string, unknown>;
    const server = rec.server && typeof rec.server === "object" ? (rec.server as Record<string, unknown>) : rec;
    const repoObj = server.repository && typeof server.repository === "object" ? (server.repository as Record<string, unknown>) : undefined;
    const repositoryUrl =
      (typeof server.repositoryUrl === "string" && server.repositoryUrl) ||
      (typeof repoObj?.url === "string" && repoObj.url) ||
      (typeof server.homepage === "string" && server.homepage) ||
      "";
    const name = typeof server.name === "string" ? server.name : typeof rec.name === "string" ? rec.name : "";
    if (!name || !repositoryUrl) {
      continue;
    }
    const publisher =
      (typeof server.publisher === "string" && server.publisher) ||
      (typeof rec.publisher === "string" && rec.publisher) ||
      name.split("/")[0] ||
      "unknown";
    out.push({
      name: name.split("/").pop() ?? name,
      description: typeof server.description === "string" ? server.description : name,
      publisher,
      repository: name.includes("/") ? name : `${publisher}/${name}`,
      repositoryUrl,
      defaultRef: typeof server.version === "string" ? server.version : "unknown",
    });
  }
  return out;
}
