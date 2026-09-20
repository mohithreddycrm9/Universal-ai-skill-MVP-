import { describe, expect, it } from "vitest";
import { DockerSandbox } from "../../src/sandbox/docker.js";
import { loadConfig } from "../../src/policy/load.js";
import { benignPackage, postinstallPackage } from "../helpers.js";
import { extractInstructions, inferRisk, manifestFromPackage } from "../../src/skills/manifest.js";
import type { SpawnFn, SpawnResult } from "../../src/util/spawn.js";

function ok(stdout = ""): SpawnResult {
  return { status: 0, stdout, stderr: "" };
}

function missing(): SpawnResult {
  return { status: 1, stdout: "", stderr: "not found", error: Object.assign(new Error("not found"), { code: "ENOENT" }) };
}

describe("DockerSandbox", () => {
  it("returns INCONCLUSIVE when Docker/Podman are missing (never PASS)", async () => {
    const spawn: SpawnFn = () => missing();
    const sandbox = new DockerSandbox(loadConfig("config").sandbox, spawn);
    const pkg = benignPackage();
    const manifest = manifestFromPackage(pkg, extractInstructions(pkg), inferRisk(pkg));
    const result = await sandbox.evaluate(pkg, manifest);
    expect(result.status).toBe("INCONCLUSIVE");
    expect(result.status).not.toBe("PASS");
    expect(result.notes).toMatch(/INCONCLUSIVE/i);
  });

  it("builds an isolated run with resource limits and no docker.sock or host credentials", () => {
    const policy = loadConfig("config").sandbox;
    const sandbox = new DockerSandbox(policy, () => missing());
    const args = sandbox.buildRunArgs("docker", "/tmp/skill", "/tmp/observer.sh");
    const joined = args.join(" ");
    expect(args).toContain("--network");
    expect(args).toContain("none");
    expect(args).toContain("--cap-drop");
    expect(args).toContain("ALL");
    expect(args).toContain("--read-only");
    expect(args).toContain("--memory");
    expect(args).toContain("--pids-limit");
    expect(args).toContain("--cpus");
    expect(joined).toContain("no-new-privileges");
    expect(joined).not.toContain("docker.sock");
    expect(joined).not.toContain("--privileged");
    expect(args).not.toContain("host");
  });

  it("records a behavioral fingerprint from an isolated observer (injected runner)", async () => {
    const calls: string[] = [];
    const spawn: SpawnFn = (command, args) => {
      calls.push(`${command} ${args.join(" ")}`);
      if (args[0] === "info" || (args[0] === "image" && args[1] === "inspect")) {
        return ok("ok");
      }
      if (args[0] === "run") {
        expect(args.join(" ")).not.toContain("docker.sock");
        return ok(
          'FINGERPRINT_JSON:{"filesRead":["SKILL.md"],"filesWritten":[],"processes":["observer"],"networkConnections":[],"environmentAccess":[],"secretsAccessed":[]}\n',
        );
      }
      return missing();
    };
    const sandbox = new DockerSandbox(loadConfig("config").sandbox, spawn);
    const pkg = benignPackage();
    const manifest = manifestFromPackage(pkg, extractInstructions(pkg), inferRisk(pkg));
    const result = await sandbox.evaluate(pkg, manifest);
    expect(result.status).toBe("PASS");
    expect(result.observed.filesRead.length).toBeGreaterThan(0);
    expect(result.observed.processes).toContain("observer");
    expect(result.notes).toMatch(/not a universal safety claim/i);
    expect(calls.some((item) => item.startsWith("docker run"))).toBe(true);
  });

  it("does not execute install hooks; static fingerprint still records them", async () => {
    const spawn: SpawnFn = (_command, args) => {
      if (args[0] === "info" || (args[0] === "image" && args[1] === "inspect")) {
        return ok("ok");
      }
      if (args[0] === "run") {
        expect(args.join(" ")).not.toContain("npm");
        return ok("FINGERPRINT_JSON:{\"filesRead\":[],\"filesWritten\":[],\"processes\":[\"observer\"],\"networkConnections\":[],\"environmentAccess\":[],\"secretsAccessed\":[]}\n");
      }
      return missing();
    };
    const sandbox = new DockerSandbox(loadConfig("config").sandbox, spawn);
    const pkg = postinstallPackage();
    const manifest = manifestFromPackage(pkg, extractInstructions(pkg), inferRisk(pkg));
    const result = await sandbox.evaluate(pkg, manifest);
    expect(result.observed.processes).toContain("npm:postinstall");
    expect(["FAIL", "PASS"]).toContain(result.status);
  });
});
