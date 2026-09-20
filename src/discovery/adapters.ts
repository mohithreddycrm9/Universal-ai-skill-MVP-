import type { SkillSource } from "./skill-source.js";
import { UnimplementedSource } from "./unimplemented.js";

export class MCPRegistrySource extends UnimplementedSource {
  constructor() {
    super("mcp_registry");
  }
}

export class OfficialVendorSource extends UnimplementedSource {
  constructor() {
    super("official_vendor");
  }
}

export class EnterpriseRegistrySource extends UnimplementedSource {
  constructor() {
    super("enterprise_registry");
  }
}

export class AgentDiscoverySource extends UnimplementedSource {
  constructor() {
    super("agent_discovery");
  }
}

export function extraSources(): SkillSource[] {
  return [
    new MCPRegistrySource(),
    new OfficialVendorSource(),
    new EnterpriseRegistrySource(),
    new AgentDiscoverySource(),
  ];
}
