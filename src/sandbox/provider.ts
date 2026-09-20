import type { BehavioralFingerprint, Capability, SkillManifest, SkillPackage } from "../types.js";
import type { SecurityStatus } from "../types.js";
import type { CostMetadata } from "../cost/types.js";

/** Current Docker stage is static observation only — it does not execute skill code. */
export type SandboxStage = "ISOLATED_STATIC";
export type SandboxMode = "SANDBOX_STATIC_ONLY";

export interface SandboxResult {
  status: SecurityStatus;
  observed: BehavioralFingerprint;
  unexpected: string[];
  notes: string;
  /** Explicit stage label for MCP/audit — not runtime detonation. */
  stage?: SandboxStage;
  /** User-facing mode: static-only sandbox; skill entrypoints/hooks are not executed. */
  sandboxMode?: SandboxMode;
  /** Always false for the current Docker/in-process adapters. */
  executesSkillCode?: boolean;
}

export interface SandboxProvider {
  readonly id: string;
  readonly cost?: CostMetadata;
  evaluate(pkg: SkillPackage, manifest: SkillManifest): Promise<SandboxResult>;
}

export function declaredCapabilities(manifest: SkillManifest): Capability[] {
  return manifest.spec.capabilitiesDeclared;
}

export function unexpectedPrivileges(
  declared: Capability[],
  observed: BehavioralFingerprint,
): string[] {
  const unexpected: string[] = [];
  if (observed.networkConnections.length && !declared.some((item) => item.startsWith("network."))) {
    unexpected.push("undeclared_network");
  }
  if (observed.processes.some((proc) => /shell|sh|bash|cmd|powershell/i.test(proc)) && !declared.includes("shell.execute")) {
    unexpected.push("undeclared_shell");
  }
  if (observed.secretsAccessed.length && !declared.includes("credential.read")) {
    unexpected.push("undeclared_credential_access");
  }
  if (observed.environmentAccess.some((key) => /TOKEN|SECRET|PASSWORD|AWS|KEY/i.test(key))) {
    unexpected.push("environment_credential_touch");
  }
  if (observed.filesWritten.some((path) => path.startsWith("/") && !path.startsWith("/tmp"))) {
    unexpected.push("host_write");
  }
  return unexpected;
}
