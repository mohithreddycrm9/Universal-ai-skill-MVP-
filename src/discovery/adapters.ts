import { join } from "node:path";
import type { SkillSource } from "./skill-source.js";
import { UnimplementedSource } from "./unimplemented.js";
import { MCPRegistrySource } from "./mcp-registry.js";
import { EnterpriseRegistrySource, OfficialVendorSource } from "./allowlist-source.js";
import { assertNever } from "../util/assert-never.js";
import type { RegistryPolicy, RegistrySource, SourceKind } from "../policy/load.js";
import { COST_CATALOG } from "../cost/catalog.js";
import type { CostMetadata, PricingModel } from "../cost/types.js";

export { MCPRegistrySource } from "./mcp-registry.js";
export { OfficialVendorSource, EnterpriseRegistrySource, AllowlistedCatalogSource } from "./allowlist-source.js";

export class AgentDiscoverySource extends UnimplementedSource {
  constructor() {
    super("agent_discovery");
  }
}

export function extraSources(configDir = "config"): SkillSource[] {
  return [
    new MCPRegistrySource({ fixturePath: join(configDir, "mcp-registry.fixture.yaml") }),
    OfficialVendorSource.fromCatalog(join(configDir, "official-vendors.yaml")),
    EnterpriseRegistrySource.fromCatalog(join(configDir, "enterprise-registry.yaml")),
  ];
}

export function sourcesFromRegistry(policy: RegistryPolicy, configDir: string): SkillSource[] {
  const out: SkillSource[] = [];
  for (const source of policy.sources) {
    if (!source.enabled) {
      continue;
    }
    const built = buildSource(source, configDir);
    if (built) {
      out.push(built);
    }
  }
  return out;
}

function buildSource(source: RegistrySource, configDir: string): SkillSource | undefined {
  const kind = source.kind ?? inferKind(source.id);
  switch (kind) {
    case "github":
    case "local":
      return undefined;
    case "mcp_registry": {
      const remoteCost = remoteCostFrom(source, COST_CATALOG.mcp_registry_remote);
      return new MCPRegistrySource({
        id: source.id,
        mode: source.mode ?? "fixture",
        fixturePath: resolveMaybe(configDir, source.fixturePath ?? "mcp-registry.fixture.yaml"),
        apiBase: source.apiBase,
        remoteCost,
      });
    }
    case "official_vendor":
      return OfficialVendorSource.fromCatalog(resolveMaybe(configDir, source.catalogPath ?? "official-vendors.yaml"));
    case "enterprise_registry":
      return EnterpriseRegistrySource.fromCatalog(resolveMaybe(configDir, source.catalogPath ?? "enterprise-registry.yaml"));
    case "agent_discovery":
      return new AgentDiscoverySource();
    default:
      return assertNever(kind, "source kind");
  }
}

function inferKind(id: string): SourceKind {
  switch (id) {
    case "github":
      return "github";
    case "mcp_registry":
    case "package_registry":
      return "mcp_registry";
    case "official_vendor":
    case "official_docs":
      return "official_vendor";
    case "enterprise_registry":
      return "enterprise_registry";
    case "agent_discovery":
      return "agent_discovery";
    case "local":
      return "local";
    default:
      return "agent_discovery";
  }
}

function resolveMaybe(configDir: string, relative: string): string {
  if (relative.startsWith("/") || /^[A-Za-z]:[\\/]/.test(relative)) {
    return relative;
  }
  return join(configDir, relative.replace(/^config\//, ""));
}

function remoteCostFrom(source: RegistrySource, fallback: CostMetadata): CostMetadata {
  const pricingModel = (source.pricingModel ?? fallback.pricingModel) as PricingModel;
  return {
    ...fallback,
    pricingModel,
    freeTier: source.freeTier ?? fallback.freeTier,
    requiresApproval: source.requiresApproval ?? fallback.requiresApproval,
    estimatedCost: source.estimatedCost ?? fallback.estimatedCost,
  };
}
