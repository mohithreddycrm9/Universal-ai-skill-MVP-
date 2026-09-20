export const LIFECYCLES = [
  "DISCOVERED",
  "QUARANTINED",
  "PROVENANCE_CHECK",
  "SECURITY_SCAN",
  "SANDBOX",
  "POLICY_EVALUATION",
  "APPROVED",
  "AVAILABLE",
  "REJECTED",
  "EXPIRED",
  "INVALIDATED",
  "UNTRUSTED",
  "VERIFYING",
  "SCANNING",
  "SANDBOXING",
] as const;
export type Lifecycle = (typeof LIFECYCLES)[number];

export const CANONICAL_LIFECYCLES = [
  "DISCOVERED",
  "QUARANTINED",
  "PROVENANCE_CHECK",
  "SECURITY_SCAN",
  "SANDBOX",
  "POLICY_EVALUATION",
  "APPROVED",
  "AVAILABLE",
  "REJECTED",
  "EXPIRED",
  "INVALIDATED",
] as const;
export type CanonicalLifecycle = (typeof CANONICAL_LIFECYCLES)[number];

export const TRUST_TIERS = [
  "OFFICIAL",
  "VERIFIED",
  "TRUSTED_COMMUNITY",
  "UNKNOWN",
  "UNTRUSTED",
] as const;
export type TrustTier = (typeof TRUST_TIERS)[number];

export const SECURITY_STATUSES = [
  "PASS",
  "FAIL",
  "INCONCLUSIVE",
  "ERROR",
  "TIMEOUT",
  "NOT_RUN",
] as const;
export type SecurityStatus = (typeof SECURITY_STATUSES)[number];

export const QUALITY_STATUSES = ["UNKNOWN", "ACCEPTABLE", "GOOD"] as const;
export type QualityStatus = (typeof QUALITY_STATUSES)[number];

export const RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export type RiskLevel = (typeof RISK_LEVELS)[number];

export const PERSISTENCE_TIERS = [
  "TEMPORARY",
  "SESSION",
  "CACHED",
  "TRUSTED",
  "PERSISTENT",
] as const;
export type PersistenceTier = (typeof PERSISTENCE_TIERS)[number];

export const FIREWALL_DECISIONS = ["ALLOW", "DENY", "REQUIRE_USER_APPROVAL"] as const;
export type FirewallDecision = (typeof FIREWALL_DECISIONS)[number];

export const CAPABILITIES = [
  "filesystem.read",
  "filesystem.write",
  "network.read",
  "network.write",
  "shell.execute",
  "process.spawn",
  "credential.read",
  "browser.control",
  "database.read",
  "database.write",
  "cloud.read",
  "cloud.write",
  "production.deploy",
] as const;
export type Capability = (typeof CAPABILITIES)[number];

export const FINDING_SEVERITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export type FindingSeverity = (typeof FINDING_SEVERITIES)[number];

export const JOB_STATES = ["QUEUED", "RUNNING", "SUCCEEDED", "FAILED", "CANCELLED"] as const;
export type JobState = (typeof JOB_STATES)[number];

export const JOB_TYPES = [
  "SKILL_DISCOVERY",
  "PROVENANCE_CHECK",
  "SECURITY_SCAN",
  "SANDBOX_TEST",
  "FULL_RESCAN",
  "DEPENDENCY_REFRESH",
  "TRUST_REFRESH",
  "SKILL_INVALIDATION",
  "ACQUIRE",
] as const;
export type JobType = (typeof JOB_TYPES)[number];

export const DISCLOSURE_LEVELS = [0, 1, 2, 3] as const;
export type DisclosureLevel = (typeof DISCLOSURE_LEVELS)[number];

export const SECURITY_NOTICE =
  "Results describe configured-check outcomes for a pinned version, not universal safety. INCONCLUSIVE is not PASS.";

export interface SkillManifest {
  apiVersion: "skill.mcp/v1";
  kind: "Skill";
  metadata: {
    name: string;
    description: string;
    publisher: string;
    repository: string;
    version: string;
    license?: string;
    tags?: string[];
  };
  spec: {
    instructions: string;
    risk: RiskLevel;
    capabilitiesDeclared: Capability[];
    entrypoints: string[];
    files: Array<{ path: string; sha256: string }>;
    dependencies: { lockHash: string };
  };
}

export interface McpSkillResource {
  path: string;
  digest: string;
  mimeType?: string;
}

export interface McpSkill {
  name: string;
  description: string;
  version: string;
  body: string;
  resources: McpSkillResource[];
}

export interface SkillRecord {
  id: string;
  name: string;
  publisher: string;
  repository: string;
  version: string;
  commitSha: string;
  fingerprint: string;
  manifest: SkillManifest;
  persistence: PersistenceTier;
  lifecycle: Lifecycle;
  trustTier: TrustTier;
  securityStatus: SecurityStatus;
  qualityStatus: QualityStatus;
  risk: RiskLevel;
  permissions: Capability[];
  sbomSummary: unknown;
  sandboxSummary: unknown;
  expirationAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Finding {
  id: string;
  severity: FindingSeverity;
  title: string;
  path?: string;
  evidence: string;
}

export interface ScannerRun {
  scannerId: string;
  scannerVersion: string;
  status: SecurityStatus;
  startedAt: string;
  finishedAt: string;
  findings: Finding[];
  notes?: string;
}

export interface SkillFile {
  path: string;
  content: string;
}

export interface SkillPackage {
  sourceId: string;
  publisher: string;
  ownerLogin: string;
  repository: string;
  repositoryUrl: string;
  commitSha: string;
  version: string;
  license?: string;
  archived: boolean;
  createdAt?: string;
  files: SkillFile[];
}

export interface SkillCandidate {
  candidateId: string;
  sourceId: string;
  name: string;
  description: string;
  publisher: string;
  repository: string;
  repositoryUrl: string;
  defaultRef: string;
  owner?: string;
  repo?: string;
  version?: string;
  commit?: string;
  metadata?: Record<string, unknown>;
}

export interface ScanTarget {
  skillId: string;
  package: SkillPackage;
  quarantinePath: string;
  changedPaths?: string[];
}

export interface FingerprintInput {
  publisher: string;
  repository: string;
  commitSha: string;
  manifestCanonical: string;
  dependencyLockHash: string;
  securityConfigurationHash: string;
  filesDigest?: string;
}

export interface BehavioralFingerprint {
  filesRead: string[];
  filesWritten: string[];
  processes: string[];
  networkConnections: string[];
  environmentAccess: string[];
  secretsAccessed: string[];
}

export function isCapability(value: string): value is Capability {
  return (CAPABILITIES as readonly string[]).includes(value);
}

export function isLifecycle(value: string): value is Lifecycle {
  return (LIFECYCLES as readonly string[]).includes(value);
}

export function assertNever(value: never, message: string): never {
  throw new Error(`${message}: ${String(value)}`);
}
