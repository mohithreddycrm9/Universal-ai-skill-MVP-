import type { BehavioralFingerprint, Capability, SkillManifest, SkillPackage } from "../types.js";
import type { SecurityStatus } from "../types.js";
import type { CostMetadata } from "../cost/types.js";

export interface SandboxResult {
  status: SecurityStatus;
  observed: BehavioralFingerprint;
  unexpected: string[];
  notes: string;
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
