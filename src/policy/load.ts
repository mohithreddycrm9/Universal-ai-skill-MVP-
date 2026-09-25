import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { parse as parseYaml } from "yaml";
import { z } from "zod";
import { RISK_LEVELS } from "../types.js";
import { canonicalize } from "../util/canonical.js";
import { sha256 } from "../util/hash.js";
import type { ScannerImplementationIdentity } from "../cache/scanner-identity.js";
import { normalizeScannerImplementationIdentities } from "../cache/scanner-identity.js";
import { COST_POLICIES, type CostPolicy } from "../cost/types.js";

const trustSchema = z.object({
  allowUnknownPublisherScan: z.boolean().default(true),
  allowUnknownPublisherApprove: z.boolean().default(false),
  treatArchivedAsUntrusted: z.boolean().default(true),
  officialOrganizations: z.array(z.string()).default([]),
  verifiedPublishers: z.array(z.string()).default([]),
  trustedCommunityPublishers: z.array(z.string()).default([]),
  untrustedPublishers: z.array(z.string()).default([]),
  officialDomainSuffixes: z.array(z.string()).default([]),
  requireSignedReleaseForOfficial: z.boolean().default(false),
});

const securitySchema = z.object({
  revalidationHours: z.number().nonnegative().default(168),
  requiredScanners: z.array(z.string()).default(["secret", "prompt_injection", "suspicious_files", "dependency"]),
  extendedScanningEnabled: z.boolean().default(false),
  optionalScanners: z.array(z.string()).default(["license"]),
  failOnSeverity: z.array(z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"])).default(["CRITICAL", "HIGH"]),
  inconclusiveRequiredIsNotPass: z.boolean().default(true),
  denyApproveOnInconclusive: z.boolean().default(true),
  denyApproveOnFail: z.boolean().default(true),
  maxFindingEvidenceChars: z.number().nonnegative().default(480),
  maxFindingsPerScanner: z.number().nonnegative().default(50),
  maxConcurrentScanners: z.number().int().positive().max(32).default(4),
  scannerTimeoutMs: z.number().int().positive().max(600_000).default(120_000),
});

const SOURCE_KINDS = [
  "github",
  "mcp_registry",
  "official_vendor",
  "enterprise_registry",
  "agent_discovery",
  "local",
] as const;
export type SourceKind = (typeof SOURCE_KINDS)[number];

const sourceSchema = z.object({
  id: z.string(),
  enabled: z.boolean().default(true),
  kind: z.enum(SOURCE_KINDS).optional(),
  apiBase: z.string().optional(),
  fixturePath: z.string().optional(),
  catalogPath: z.string().optional(),
  mode: z.enum(["fixture", "remote"]).default("fixture"),
  pricingModel: z.enum(["free", "freemium", "paid", "usage_based", "unknown"]).optional(),
  freeTier: z.boolean().optional(),
  requiresApproval: z.boolean().optional(),
  estimatedCost: z.string().optional(),
});
export type RegistrySource = z.infer<typeof sourceSchema>;

const sandboxSchema = z.object({
  runtime: z.string().default("docker"),
  image: z.string().default("busybox:1.36"),
  network: z.string().default("none"),
  readOnlyRoot: z.boolean().default(true),
  capDrop: z.array(z.string()).default(["ALL"]),
  noNewPrivileges: z.boolean().default(true),
  memoryMb: z.number().positive().default(256),
  cpus: z.number().positive().default(0.5),
  pidsLimit: z.number().int().positive().default(64),
  timeoutSeconds: z.number().positive().default(30),
  tmpfs: z.array(z.string()).default(["/tmp"]),
  forbiddenMounts: z.array(z.string()).default(["/var/run/docker.sock"]),
  forbiddenEnvPatterns: z.array(z.string()).default(["*TOKEN*", "*SECRET*"]),
  executableRequiresSandbox: z.boolean().default(true),
  unexpectedPrivilegeQuarantine: z.boolean().default(true),
  pullImage: z.boolean().default(false),
  cloudSandboxEnabled: z.boolean().default(false),
});

const scannerSchema = z.object({
  scanners: z
    .record(
      z.string(),
      z
        .object({
          enabled: z.boolean().default(true),
          binary: z.string().optional(),
          failOpen: z.boolean().optional(),
          typosquatDistance: z.number().nonnegative().optional(),
          allowed: z.array(z.string()).optional(),
        })
        .passthrough(),
    )
    .default({}),
});

const registrySchema = z.object({
  database: z
    .object({
      driver: z.enum(["sqlite", "postgres"]).default("sqlite"),
      sqlitePath: z.string().default("data/skill-mcp.sqlite"),
      url: z.string().optional(),
    })
    .default({ driver: "sqlite", sqlitePath: "data/skill-mcp.sqlite" }),
  sources: z.array(sourceSchema).default([]),
  response: z.object({ maxBytes: z.number().positive().default(32768) }).default({ maxBytes: 32768 }),
  dataDir: z.string().default("data"),
  quarantineDir: z.string().default("data/quarantine"),
});

const costSchema = z.object({
  policy: z.enum(COST_POLICIES).default("ALLOW_FREE_ONLY"),
  currency: z.string().default("USD"),
  allowUpToAmount: z.number().nonnegative().default(0),
  preferFreeAlternatives: z.boolean().default(true),
  neverAutoPaidFallback: z.boolean().default(true),
  unknownCostRequiresApproval: z.boolean().default(true),
});
export type TrustPolicy = z.infer<typeof trustSchema>;
export type SecurityPolicy = z.infer<typeof securitySchema>;
export type SandboxPolicy = z.infer<typeof sandboxSchema>;
export type ScannerPolicy = z.infer<typeof scannerSchema>;
export type RegistryPolicy = z.infer<typeof registrySchema>;

export interface AppConfig {
  trust: TrustPolicy;
  security: SecurityPolicy;
  sandbox: SandboxPolicy;
  scanners: ScannerPolicy;
  registry: RegistryPolicy;
  cost: CostPolicy;
  configDir: string;
}

export function loadConfig(configDir: string): AppConfig {
  return {
    configDir,
    trust: loadYaml(join(configDir, "trust-policy.yaml"), trustSchema),
    security: loadYaml(join(configDir, "security-policy.yaml"), securitySchema),
    sandbox: loadYaml(join(configDir, "sandbox-policy.yaml"), sandboxSchema),
    scanners: loadYaml(join(configDir, "scanner-policy.yaml"), scannerSchema),
    registry: loadYaml(join(configDir, "registry.yaml"), registrySchema),
    cost: loadYaml(join(configDir, "cost-policy.yaml"), costSchema),
  };
}

function loadYaml<T>(path: string, schema: z.ZodType<T>): T {
  if (!existsSync(path)) {
    return schema.parse({});
  }
  const raw = parseYaml(readFileSync(path, "utf8")) ?? {};
  return schema.parse(raw);
}

/**
 * Canonical security-configuration identity for fingerprints / verification cache.
 * Includes policy knobs, enabled-scanner config, and material scanner
 * implementation versions (name+version). Scanner upgrade/downgrade/add/remove
 * changes this hash → cache miss. Folded here so fingerprint stays the single
 * cache key (no redundant parallel hash).
 */
export function securityConfigurationHash(
  config: AppConfig,
  materialScanners: readonly ScannerImplementationIdentity[] = [],
): string {
  const materialScannerImplementations = normalizeScannerImplementationIdentities(materialScanners).map(
    (item) => ({ name: item.name, version: item.version }),
  );
  return sha256(
    canonicalize({
      requiredScanners: config.security.requiredScanners,
      optionalScanners: config.security.optionalScanners,
      extendedScanningEnabled: config.security.extendedScanningEnabled,
      failOnSeverity: config.security.failOnSeverity,
      scannerEnabled: config.scanners.scanners,
      materialScannerImplementations,
    }),
  );
}

