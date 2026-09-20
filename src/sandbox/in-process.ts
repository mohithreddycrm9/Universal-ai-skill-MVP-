import type { SkillManifest, SkillPackage } from "../types.js";
import { hasInstallScripts } from "../skills/manifest.js";
import type { SandboxProvider, SandboxResult } from "./provider.js";
import { unexpectedPrivileges } from "./provider.js";
import { COST_CATALOG } from "../cost/catalog.js";

/**
 * ISOLATED_STATIC / SANDBOX_STATIC_ONLY test adapter: does not execute untrusted code.
 * Interprets package metadata into an observed behavioral fingerprint (not detonation).
 */
export class InProcessSandbox implements SandboxProvider {
  readonly id = "in-process";
  readonly cost = COST_CATALOG.in_process_sandbox;

  async evaluate(pkg: SkillPackage, manifest: SkillManifest): Promise<SandboxResult> {
    const observed = {
      filesRead: pkg.files.map((file) => file.path),
      filesWritten: [] as string[],
      processes: [] as string[],
      networkConnections: [] as string[],
      environmentAccess: [] as string[],
      secretsAccessed: [] as string[],
    };
    if (hasInstallScripts(pkg)) {
      observed.processes.push("npm:postinstall");
      observed.networkConnections.push("pkg:install-hook");
    }
    const body = pkg.files.map((file) => file.content).join("\n");
    if (/https?:\/\//i.test(body) && /curl|fetch\(|wget|http\.request/i.test(body)) {
      observed.networkConnections.push("static:http-client");
    }
    if (/child_process|\/bin\/sh/.test(body)) {
      observed.processes.push("shell");
    }
    const unexpected = unexpectedPrivileges(manifest.spec.capabilitiesDeclared, observed);
    const executable = manifest.spec.entrypoints.length > 0 || hasInstallScripts(pkg);
    if (!executable) {
      return {
        status: "PASS",
        observed,
        unexpected: [],
        notes: "ISOLATED_STATIC/SANDBOX_STATIC_ONLY: No executable components; static observation not required. Does not run skill code. Not a malware-free claim.",
        stage: "ISOLATED_STATIC",
        sandboxMode: "SANDBOX_STATIC_ONLY",
        executesSkillCode: false,
      };
    }
    if (unexpected.length) {
      return {
        status: "FAIL",
        observed,
        unexpected,
        notes: `ISOLATED_STATIC/SANDBOX_STATIC_ONLY: Unexpected privileged signals in static observation: ${unexpected.join(",")}. Skill code was not executed.`,
        stage: "ISOLATED_STATIC",
        sandboxMode: "SANDBOX_STATIC_ONLY",
        executesSkillCode: false,
      };
    }
    return {
      status: "PASS",
      observed,
      unexpected,
      notes: "ISOLATED_STATIC/SANDBOX_STATIC_ONLY: In-process static observation of declared vs observed. Does NOT execute skill code/entrypoints/hooks. Not runtime detonation.",
      stage: "ISOLATED_STATIC",
      sandboxMode: "SANDBOX_STATIC_ONLY",
      executesSkillCode: false,
    };
  }
}
