import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { BehavioralFingerprint, SkillManifest, SkillPackage } from "../types.js";
import type { SandboxProvider, SandboxResult } from "./provider.js";
import { unexpectedPrivileges } from "./provider.js";
import type { SandboxPolicy } from "../policy/load.js";
import { COST_CATALOG } from "../cost/catalog.js";
import { defaultSpawn, type SpawnFn } from "../util/spawn.js";
import { hasInstallScripts } from "../skills/manifest.js";

const HOST_CREDENTIAL_ENV = /TOKEN|SECRET|PASSWORD|AWS|GOOGLE|AZURE|PRIVATE_KEY|GITHUB|NPM_TOKEN|KUBE/i;

/**
 * ISOLATED_STATIC / SANDBOX_STATIC_ONLY Docker/Podman adapter.
 * Runs a read-only observer inside an isolated container — does NOT execute skill
 * code, entrypoints, or install hooks (not runtime detonation).
 * Missing runtime → INCONCLUSIVE, never PASS.
 * Local engine is free. Cloud/hosted sandboxes are not constructed here.
 */
export class DockerSandbox implements SandboxProvider {
  readonly id = "docker";
  readonly cost = COST_CATALOG.docker_local;

  constructor(
    private readonly policy: SandboxPolicy,
    private readonly spawn: SpawnFn = defaultSpawn,
  ) {}

  async evaluate(pkg: SkillPackage, manifest: SkillManifest): Promise<SandboxResult> {
    const runtime = this.detectRuntime();
    if (!runtime) {
      return {
        status: "INCONCLUSIVE",
        observed: emptyBehavior(),
        unexpected: [],
        notes: "ISOLATED_STATIC/SANDBOX_STATIC_ONLY: Docker/Podman unavailable. Static sandbox not executed. Does not run skill code. INCONCLUSIVE ≠ PASS.",
        stage: "ISOLATED_STATIC",
        sandboxMode: "SANDBOX_STATIC_ONLY",
        executesSkillCode: false,
      };
    }

    const env = isolatedHostEnv();
    const inspect = this.spawn(runtime, ["image", "inspect", this.policy.image], { timeout: 4000, env });
    if (inspect.error || inspect.status !== 0) {
      return {
        status: "INCONCLUSIVE",
        observed: emptyBehavior(),
        unexpected: [],
        notes: `ISOLATED_STATIC/SANDBOX_STATIC_ONLY: Image '${this.policy.image}' is not present locally and pull is disabled by default. Does not run skill code. INCONCLUSIVE ≠ PASS.`,
        stage: "ISOLATED_STATIC",
        sandboxMode: "SANDBOX_STATIC_ONLY",
        executesSkillCode: false,
      };
    }

    const work = mkdtempSync(join(tmpdir(), "skill-mcp-sandbox-"));
    try {
      const skillDir = join(work, "skill");
      mkdirSync(skillDir, { recursive: true });
      for (const file of pkg.files) {
        const safe = file.path.replaceAll("..", "_").replaceAll("/", "_");
        writeFileSync(join(skillDir, safe), file.content, "utf8");
      }
      const observerPath = join(work, "observer.sh");
      writeFileSync(observerPath, OBSERVER_SCRIPT, { encoding: "utf8", mode: 0o755 });

      const args = this.buildRunArgs(runtime, skillDir, observerPath);
      this.assertIsolation(args, env);

      const timeoutMs = Math.max(2, this.policy.timeoutSeconds) * 1000;
      const run = this.spawn(runtime, args, { timeout: timeoutMs + 1000, env });
      if (run.error?.code === "ETIMEDOUT" || run.signal === "SIGTERM") {
        return {
          status: "TIMEOUT",
          observed: emptyBehavior(),
          unexpected: [],
          notes: "ISOLATED_STATIC/SANDBOX_STATIC_ONLY: Static sandbox timed out. Does not run skill entrypoints/hooks. INCONCLUSIVE/TIMEOUT ≠ PASS.",
          stage: "ISOLATED_STATIC",
          sandboxMode: "SANDBOX_STATIC_ONLY",
          executesSkillCode: false,
        };
      }
      if (run.error || run.status !== 0) {
        return {
          status: "INCONCLUSIVE",
          observed: emptyBehavior(),
          unexpected: [],
          notes: `ISOLATED_STATIC/SANDBOX_STATIC_ONLY: Container observer failed (${run.status ?? run.error?.message ?? "unknown"}). Does not run skill code. INCONCLUSIVE ≠ PASS.`,
          stage: "ISOLATED_STATIC",
          sandboxMode: "SANDBOX_STATIC_ONLY",
          executesSkillCode: false,
        };
      }

      const observed = mergeBehavior(parseObserverOutput(run.stdout), staticObservation(pkg));
      if (this.policy.network === "none") {
        observed.networkConnections = observed.networkConnections.filter((item) => item.startsWith("static:"));
      }
      const unexpected = unexpectedPrivileges(manifest.spec.capabilitiesDeclared, observed);
      if (unexpected.length) {
        return {
          status: "FAIL",
          observed,
          unexpected,
          notes: `ISOLATED_STATIC/SANDBOX_STATIC_ONLY: Unexpected privileged signals during static observation: ${unexpected.join(",")}. Skill entrypoints were not executed.`,
          stage: "ISOLATED_STATIC",
          sandboxMode: "SANDBOX_STATIC_ONLY",
          executesSkillCode: false,
        };
      }
      return {
        status: "PASS",
        observed,
        unexpected: [],
        notes:
          "ISOLATED_STATIC / SANDBOX_STATIC_ONLY: Isolated container completed static observation for this pinned package. Does NOT execute skill code, entrypoints, or install hooks. Not runtime detonation. Not a universal safety claim.",
        stage: "ISOLATED_STATIC",
        sandboxMode: "SANDBOX_STATIC_ONLY",
        executesSkillCode: false,
      };
    } finally {
      rmSync(work, { recursive: true, force: true });
    }
  }

  /** Public so tests can assert isolation flags without executing a container. */
  buildRunArgs(_runtime: "docker" | "podman", skillDir: string, observerPath: string): string[] {
    const network = this.policy.network && this.policy.network !== "host" ? this.policy.network : "none";
    const args = [
      "run",
      "--rm",
      "--network",
      network,
      "--read-only",
      "--cap-drop",
      "ALL",
      "--security-opt",
      "no-new-privileges",
      "--memory",
      `${this.policy.memoryMb}m`,
      "--cpus",
      String(this.policy.cpus),
      "--pids-limit",
      String(this.policy.pidsLimit),
      "--user",
      "65534:65534",
      "-v",
      `${skillDir}:/skill:ro`,
      "-v",
      `${observerPath}:/observer.sh:ro`,
    ];
    if (this.policy.tmpfs.length) {
      for (const mount of this.policy.tmpfs) {
        args.push("--tmpfs", `${mount}:rw,noexec,nosuid,size=64m`);
      }
    }
    args.push(this.policy.image, "/bin/sh", "/observer.sh");
    return args;
  }

  detectRuntime(): "docker" | "podman" | undefined {
    const env = isolatedHostEnv();
    const docker = this.spawn("docker", ["info"], { timeout: 4000, env });
    if (!docker.error && docker.status === 0) {
      return "docker";
    }
    const podman = this.spawn("podman", ["info"], { timeout: 4000, env });
    if (!podman.error && podman.status === 0) {
      return "podman";
    }
    return undefined;
  }

  private assertIsolation(args: string[], env: NodeJS.ProcessEnv): void {
    const joined = args.join(" ");
    for (const mount of this.policy.forbiddenMounts) {
      if (joined.includes(mount)) {
        throw new Error(`Sandbox would mount forbidden path ${mount}`);
      }
    }
    if (joined.includes("docker.sock") || joined.includes("--privileged") || /\s-v\s+\/:/i.test(joined)) {
      throw new Error("Sandbox isolation invariant violated");
    }
    if (args.includes("--network") && args[args.indexOf("--network") + 1] === "host") {
      throw new Error("Host network is not allowed");
    }
    for (const key of Object.keys(env)) {
      if (HOST_CREDENTIAL_ENV.test(key)) {
        throw new Error(`Sandbox host env must not forward ${key}`);
      }
    }
  }
}

function isolatedHostEnv(): NodeJS.ProcessEnv {
  return {
    PATH: process.env.PATH ?? "/usr/bin:/bin",
    LANG: "C",
    HOME: tmpdir(),
  };
}

function emptyBehavior(): BehavioralFingerprint {
  return {
    filesRead: [],
    filesWritten: [],
    processes: [],
    networkConnections: [],
    environmentAccess: [],
    secretsAccessed: [],
  };
}

function staticObservation(pkg: SkillPackage): BehavioralFingerprint {
  const observed = emptyBehavior();
  observed.filesRead = pkg.files.map((file) => file.path);
  observed.processes.push("observer");
  if (hasInstallScripts(pkg)) {
    observed.processes.push("npm:postinstall");
    observed.networkConnections.push("static:install-hook");
  }
  const body = pkg.files.map((file) => file.content).join("\n");
  if (/https?:\/\//i.test(body) && /curl|fetch\(|wget|http\.request/i.test(body)) {
    observed.networkConnections.push("static:http-client");
  }
  if (/child_process|\/bin\/sh/.test(body)) {
    observed.processes.push("shell");
  }
  return observed;
}

function parseObserverOutput(stdout: string): BehavioralFingerprint {
  const match = stdout.match(/FINGERPRINT_JSON:(.*)$/m);
  if (!match?.[1]) {
    const observed = emptyBehavior();
    observed.processes.push("observer");
    if (/NETWORK_OK/.test(stdout)) {
      observed.networkConnections.push("container:network");
    }
    return observed;
  }
  try {
    const parsed = JSON.parse(match[1]) as Partial<BehavioralFingerprint>;
    return {
      filesRead: parsed.filesRead ?? [],
      filesWritten: parsed.filesWritten ?? [],
      processes: parsed.processes ?? ["observer"],
      networkConnections: parsed.networkConnections ?? [],
      environmentAccess: parsed.environmentAccess ?? [],
      secretsAccessed: parsed.secretsAccessed ?? [],
    };
  } catch {
    return emptyBehavior();
  }
}

function mergeBehavior(a: BehavioralFingerprint, b: BehavioralFingerprint): BehavioralFingerprint {
  const uniq = (items: string[]) => [...new Set(items)];
  return {
    filesRead: uniq([...a.filesRead, ...b.filesRead]),
    filesWritten: uniq([...a.filesWritten, ...b.filesWritten]),
    processes: uniq([...a.processes, ...b.processes]),
    networkConnections: uniq([...a.networkConnections, ...b.networkConnections]),
    environmentAccess: uniq([...a.environmentAccess, ...b.environmentAccess]),
    secretsAccessed: uniq([...a.secretsAccessed, ...b.secretsAccessed]),
  };
}

const OBSERVER_SCRIPT = `#!/bin/sh
# Read-only observer. Does not execute skill install hooks or entrypoints.
echo FINGERPRINT_BEGIN
ls /skill 2>/dev/null || true
if command -v wget >/dev/null 2>&1; then
  wget -q -T 1 -O /dev/null http://127.0.0.1:9 && echo NETWORK_OK || echo NETWORK_DENIED
else
  echo NETWORK_DENIED
fi
echo FINGERPRINT_JSON:{"filesRead":[],"filesWritten":[],"processes":["observer"],"networkConnections":[],"environmentAccess":[],"secretsAccessed":[]}
echo FINGERPRINT_END
`;
