import type {
  Capability,
  DisclosureLevel,
  McpSkill,
  SecurityStatus,
  SkillRecord,
  TrustTier,
} from "../types.js";
import { SECURITY_NOTICE } from "../types.js";
import { describeLifecycle, normalizeLifecycle } from "./lifecycle.js";
import { truncate } from "./manifest.js";

export interface DisclosureCard {
  level: DisclosureLevel;
  name: string;
  description: string;
  version: string;
  trust: TrustTier;
  security: SecurityStatus;
  fingerprint: string;
  lifecycle: string;
  securityNotice: string;
  authorized: boolean;
  permissions?: Capability[];
  skillMd?: string;
  resources?: Array<{ path: string; digest: string }>;
  resource?: { path: string; content: string; digest: string };
  lifecycleNote: string;
  reputation?: string;
}

export function disclose(
  record: SkillRecord,
  mcp: McpSkill,
  level: DisclosureLevel,
  resourcePath?: string,
  resourceBody?: string,
): DisclosureCard {
  const authorized = normalizeLifecycle(record.lifecycle) === "AVAILABLE";
  const base: DisclosureCard = {
    level,
    name: mcp.name,
    description: mcp.description,
    version: mcp.version,
    trust: record.trustTier,
    security: record.securityStatus,
    fingerprint: record.fingerprint,
    lifecycle: normalizeLifecycle(record.lifecycle),
    securityNotice: SECURITY_NOTICE,
    authorized,
    lifecycleNote: describeLifecycle(record.lifecycle),
    reputation: record.qualityStatus,
  };
  if (level === 0) {
    return base;
  }
  if (level === 1) {
    return { ...base, permissions: record.permissions, skillMd: truncate(mcp.body, 8 * 1024) };
  }
  if (level === 2) {
    return {
      ...base,
      permissions: record.permissions,
      skillMd: truncate(mcp.body, 8 * 1024),
      resources: mcp.resources.map((item) => ({ path: item.path, digest: item.digest })),
    };
  }
  const wanted = resourcePath ?? mcp.resources[0]?.path;
  const hit = mcp.resources.find((item) => item.path === wanted);
  return {
    ...base,
    permissions: record.permissions,
    skillMd: truncate(mcp.body, 8 * 1024),
    resources: mcp.resources.map((item) => ({ path: item.path, digest: item.digest })),
    resource: hit
      ? {
          path: hit.path,
          digest: hit.digest,
          content: truncate(resourceBody ?? "", 8 * 1024),
        }
      : undefined,
  };
}
