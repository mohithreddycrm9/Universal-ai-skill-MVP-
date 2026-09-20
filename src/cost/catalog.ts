import type { CostMetadata } from "./types.js";

const localFree = (
  service: string,
  purpose: string,
  extra?: Partial<CostMetadata>,
): CostMetadata => ({
  provider: "local",
  service,
  pricingModel: "free",
  freeTier: true,
  estimatedCost: "0",
  requiresApproval: false,
  purpose,
  ...extra,
});

export const COST_CATALOG = {
  local_source: localFree("LocalRegistrySource", "Read skills already on disk / in-process fixtures"),
  sqlite: localFree("SQLite registry", "Persist skills, jobs, audit, verification cache"),
  in_process_sandbox: localFree("In-process sandbox evaluator", "Static declared-vs-observed checks; does not execute untrusted code"),
  docker_local: localFree("Docker/Podman local engine", "Optional local container isolation (not a cloud account)", {
    freeAlternative: "In-process evaluator for documentation-only skills",
  }),
  secret: localFree("Built-in secret scanner", "Detect secret-shaped strings locally"),
  prompt_injection: localFree("Built-in prompt-injection scanner", "Deterministic pattern scan; not an LLM"),
  suspicious_files: localFree("Built-in suspicious-file scanner", "Pattern scan of quarantined files"),
  dependency: localFree("Built-in dependency/SBOM scanner", "Parse manifests and emit a compact SBOM summary"),
  license: localFree("Built-in license scanner", "Compare declared licenses to policy"),
  strix: localFree("STRIX CLI (OSS, if installed)", "Optional local STRIX binary. Missing binary is ERROR, never PASS.", {
    freeAlternative: "Built-in scanners (secret, injection, dependency)",
  }),
  semgrep: localFree("Semgrep CLI (OSS, if installed)", "Optional local SAST. Missing binary is ERROR, never PASS."),
  gitleaks: localFree("Gitleaks CLI (OSS, if installed)", "Optional local secret scan. Missing binary is ERROR, never PASS."),
  trivy: localFree("Trivy CLI (OSS, if installed)", "Optional local vuln/SBOM scan. Missing binary is ERROR, never PASS."),
  clamav: localFree("ClamAV (OSS, if installed)", "Optional local malware signatures. Missing binary is ERROR, never PASS."),
  osv: localFree("OSV-Scanner (OSS, if installed)", "Optional local advisory scan. Missing binary is ERROR, never PASS."),
  syft: localFree("Syft (OSS, if installed)", "Optional local SBOM. Missing binary is ERROR, never PASS."),
  github_public: {
    provider: "GitHub",
    service: "Public REST API",
    pricingModel: "freemium" as const,
    freeTier: true,
    estimatedCost: "0 for public repository REST within documented free rate limits",
    requiresApproval: false,
    purpose: "Discover and fetch a bounded public file subset (not a repo dump)",
    freeAlternative: "LocalRegistrySource",
  },
  github_private_or_unknown: {
    provider: "GitHub",
    service: "Private or non-public GitHub API",
    pricingModel: "unknown" as const,
    freeTier: false,
    estimatedCost: "unknown",
    requiresApproval: true,
    purpose: "Fetch a private or billed GitHub API surface",
    freeAlternative: "Public repositories or a local skill package",
  },
  snyk: {
    provider: "Snyk",
    service: "Snyk CLI / mcp-scan",
    pricingModel: "paid" as const,
    freeTier: false,
    estimatedCost: "unknown",
    requiresApproval: true,
    purpose: "Optional commercial vulnerability scan",
    freeAlternative: "OSV-Scanner, Trivy, Syft, built-in dependency scanner",
  },
  unimplemented_remote: {
    provider: "unspecified",
    service: "Remote registry stub",
    pricingModel: "unknown" as const,
    freeTier: false,
    estimatedCost: "unknown",
    requiresApproval: true,
    purpose: "Reserved SkillSource (MCP registry / vendor / enterprise)",
    freeAlternative: "LocalRegistrySource or public GitHub",
  },
} as const satisfies Record<string, CostMetadata>;

export type CatalogKey = keyof typeof COST_CATALOG;
