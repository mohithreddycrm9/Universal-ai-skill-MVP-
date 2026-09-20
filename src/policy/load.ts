import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { parse as parseYaml } from "yaml";
import { z } from "zod";
import { RISK_LEVELS, type RiskLevel } from "../types.js";
import { canonicalize } from "../util/canonical.js";
import { sha256 } from "../util/hash.js";
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
  revalidationHours: z.number().default(168),
  requiredScanners: z.array(z.string()).default(["secret", "prompt_injection", "suspicious_files", "dependency"]),
  optionalScanners: z.array(z.string()).default(["license", "strix"]),
  strixRequiredForRisk: z.array(z.enum(RISK_LEVELS)).default(["HIGH", "CRITICAL"]),
  failOnSeverity: z.array(z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"])).default(["CRITICAL", "HIGH"]),
  inconclusiveRequiredIsNotPass: z.boolean().default(true),
  denyApproveOnInconclusive: z.boolean().default(true),
  denyApproveOnFail: z.boolean().default(true),
  maxFindingEvidenceChars: z.number().default(480),
  maxFindingsPerScanner: z.number().default(50),
});

const sandboxSchema = z.object({
  runtime: z.string().default("docker"),
  image: z.string().default("busybox:1.36"),
  network: z.string().default("none"),
  readOnlyRoot: z.boolean().default(true),
  capDrop: z.array(z.string()).default(["ALL"]),
  noNewPrivileges: z.boolean().default(true),
  memoryMb: z.number().default(256),
  cpus: z.number().default(0.5),
  pidsLimit: z.number().default(64),
  timeoutSeconds: z.number().default(30),
  tmpfs: z.array(z.string()).default(["/tmp"]),
  forbiddenMounts: z.array(z.string()).default(["/var/run/docker.sock"]),
  forbiddenEnvPatterns: z.array(z.string()).default(["*TOKEN*", "*SECRET*"]),
  executableRequiresSandbox: z.boolean().default(true),
  unexpectedPrivilegeQuarantine: z.boolean().default(true),
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
          typosquatDistance: z.number().optional(),
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
    })
    .default({ driver: "sqlite", sqlitePath: "data/skill-mcp.sqlite" }),
  sources: z
    .array(
      z.object({
        id: z.string(),
        enabled: z.boolean().default(true),
        apiBase: z.string().optional(),
      }),
    )
    .default([]),
  response: z.object({ maxBytes: z.number().default(32768) }).default({ maxBytes: 32768 }),
  dataDir: z.string().default("data"),
  quarantineDir: z.string().default("data/quarantine"),
});

const costSchema = z.object({
  policy: z.enum(COST_POLICIES).default("ASK_BEFORE_ANY_PAID_OPERATION"),
  currency: z.string().default("USD"),
  allowUpToAmount: z.number().default(0),
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

export function securityConfigurationHash(config: AppConfig): string {
  return sha256(
    canonicalize({
      requiredScanners: config.security.requiredScanners,
      optionalScanners: config.security.optionalScanners,
      strixRequiredForRisk: config.security.strixRequiredForRisk,
      failOnSeverity: config.security.failOnSeverity,
      scannerEnabled: config.scanners.scanners,
    }),
  );
}

export function isStrixRequired(risk: RiskLevel, policy: SecurityPolicy): boolean {
  return policy.strixRequiredForRisk.includes(risk);
}
