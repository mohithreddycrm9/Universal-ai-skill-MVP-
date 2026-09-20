import { spawnSync } from "node:child_process";
import type { SkillManifest, SkillPackage } from "../types.js";
import type { SandboxProvider, SandboxResult } from "./provider.js";
import type { SandboxPolicy } from "../policy/load.js";
import { COST_CATALOG } from "../cost/catalog.js";

/**
 * Container sandbox. Missing Docker → ERROR/INCONCLUSIVE, never PASS.
 * Does not mount the Docker socket or forward host credentials.
 */
export class DockerSandbox implements SandboxProvider {
  readonly id = "docker";
  readonly cost = COST_CATALOG.docker_local;

  constructor(private readonly policy: SandboxPolicy) {}

  async evaluate(pkg: SkillPackage, _manifest: SkillManifest): Promise<SandboxResult> {
    const info = spawnSync("docker", ["info"], { encoding: "utf8", timeout: 4000 });
    if (info.error || info.status !== 0) {
      return {
        status: "ERROR",
        observed: emptyBehavior(),
        unexpected: [],
        notes: "Docker runtime unavailable. Sandbox not executed. This is not PASS.",
      };
    }
    if (this.policy.forbiddenMounts.includes("/var/run/docker.sock")) {
      // policy-enforced: we never pass -v /var/run/docker.sock
    }
    return {
      status: "INCONCLUSIVE",
      observed: emptyBehavior(),
      unexpected: [],
      notes: `Docker present; ephemeral run not executed in this build for untrusted package ${pkg.repository}@${pkg.commitSha}. INCONCLUSIVE ≠ PASS.`,
    };
  }
}

function emptyBehavior() {
  return {
    filesRead: [] as string[],
    filesWritten: [] as string[],
    processes: [] as string[],
    networkConnections: [] as string[],
    environmentAccess: [] as string[],
    secretsAccessed: [] as string[],
  };
}
