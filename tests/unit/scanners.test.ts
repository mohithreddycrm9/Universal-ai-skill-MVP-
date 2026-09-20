import { describe, expect, it } from "vitest";
import { SecretScanner } from "../../src/scanners/secret.js";
import { PromptInjectionScanner } from "../../src/scanners/prompt-injection.js";
import { StrixScanner } from "../../src/scanners/strix.js";
import { federate } from "../../src/security/orchestrator.js";
import { loadConfig } from "../../src/policy/load.js";
import { injectionPackage, secretPackage } from "../helpers.js";
import type { ScanTarget } from "../../src/types.js";

function target(pkg: ReturnType<typeof secretPackage>): ScanTarget {
  return { skillId: "skl_test", package: pkg, quarantinePath: "/tmp/q" };
}

describe("scanners", () => {
  it("detects and redacts secrets without echoing the full key", async () => {
    const run = await new SecretScanner().scan(target(secretPackage()), { enabled: true });
    expect(run.status).toBe("FAIL");
    expect(JSON.stringify(run.findings)).not.toMatch(/AKIAIOSFODNN7EXAMPLE/);
  });

  it("flags prompt injection in SKILL.md", async () => {
    const run = await new PromptInjectionScanner().scan(target(injectionPackage()), { enabled: true });
    expect(run.status).toBe("FAIL");
  });

  it("records STRIX unavailability as ERROR, never PASS", async () => {
    const run = await new StrixScanner().scan(target(secretPackage()), { binary: "strix-not-installed-xyz", failOpen: false });
    expect(run.status).toBe("ERROR");
    expect(run.status).not.toBe("PASS");
    expect(run.notes ?? "").toMatch(/not PASS/i);
  });

  it("does not convert INCONCLUSIVE required scanners into PASS", () => {
    const config = loadConfig("config");
    const result = federate(
      [
        {
          scannerId: "secret",
          scannerVersion: "1",
          status: "PASS",
          findings: [],
          startedAt: "",
          finishedAt: "",
        },
        {
          scannerId: "prompt_injection",
          scannerVersion: "1",
          status: "PASS",
          findings: [],
          startedAt: "",
          finishedAt: "",
        },
        {
          scannerId: "suspicious_files",
          scannerVersion: "1",
          status: "PASS",
          findings: [],
          startedAt: "",
          finishedAt: "",
        },
        {
          scannerId: "dependency",
          scannerVersion: "1",
          status: "INCONCLUSIVE",
          findings: [],
          startedAt: "",
          finishedAt: "",
        },
      ],
      "LOW",
      config,
    );
    expect(result.status).toBe("INCONCLUSIVE");
    expect(result.claim).toBe("INCONCLUSIVE");
  });
});
