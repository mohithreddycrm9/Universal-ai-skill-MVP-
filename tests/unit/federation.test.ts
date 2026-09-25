import { describe, expect, it } from "vitest";
import { federate } from "../../src/security/orchestrator.js";
import { loadConfig } from "../../src/policy/load.js";
import type { FindingSeverity, ScannerRun, SecurityStatus } from "../../src/types.js";

function run(
  scannerId: string,
  status: SecurityStatus,
  findings: Array<{ severity: FindingSeverity }> = [],
): ScannerRun {
  return {
    scannerId,
    scannerVersion: "1",
    status,
    findings: findings.map((f, i) => ({
      id: `${scannerId}-${i}`,
      severity: f.severity,
      title: "finding",
      evidence: "redacted",
    })),
    startedAt: "",
    finishedAt: "",
  };
}

function requiredPass(config = loadConfig("config")): ScannerRun[] {
  return config.security.requiredScanners.map((id) => run(id, "PASS"));
}

describe("scanner federation — every executed scanner contributes", () => {
  const config = loadConfig("config");

  it("non-required FAIL → aggregate FAIL", () => {
    const result = federate([...requiredPass(config), run("license", "FAIL")], "LOW", config);
    expect(result.status).toBe("FAIL");
    expect(result.claim).toBe("FAILED");
  });

  it("non-required CRITICAL finding → aggregate FAIL", () => {
    const result = federate(
      [...requiredPass(config), run("license", "PASS", [{ severity: "CRITICAL" }])],
      "LOW",
      config,
    );
    expect(result.status).toBe("FAIL");
  });

  it("non-required ERROR on LOW risk → aggregate PASS (optional outage is not blocking)", () => {
    expect(federate([...requiredPass(config), run("license", "ERROR")], "LOW", config).status).toBe(
      "PASS",
    );
  });

  it("non-required TIMEOUT on LOW risk → aggregate PASS", () => {
    expect(federate([...requiredPass(config), run("strix", "TIMEOUT")], "LOW", config).status).toBe(
      "PASS",
    );
  });

  it("STRIX required for HIGH risk: ERROR → INCONCLUSIVE", () => {
    expect(
      federate([...requiredPass(config), run("strix", "ERROR")], "HIGH", config).status,
    ).toBe("INCONCLUSIVE");
    expect(federate([...requiredPass(config), run("strix", "ERROR")], "HIGH", config).requiredMissing).toEqual(
      [],
    );
  });

  it("required missing → coverage failure (INCONCLUSIVE, never PASS)", () => {
    const required = config.security.requiredScanners;
    const runs = required.slice(1).map((id) => run(id, "PASS"));
    const result = federate(runs, "LOW", config);
    expect(result.status).toBe("INCONCLUSIVE");
    expect(result.requiredMissing).toContain(required[0]);
    expect(result.status).not.toBe("PASS");
  });

  it("disabled / absent optional scanner has no effect", () => {
    const result = federate(requiredPass(config), "LOW", config);
    expect(result.status).toBe("PASS");
    expect(result.requiredMissing).toEqual([]);
  });

  it("$0 commercial blocked stays NOT_RUN and does not PASS the gate alone", () => {
    const runs = [...requiredPass(config), run("snyk", "NOT_RUN")];
    const result = federate(runs, "LOW", config);
    expect(result.status).toBe("PASS");
    expect(runs.find((r) => r.scannerId === "snyk")?.status).toBe("NOT_RUN");
  });

  it("deterministic strongest result: FAIL beats INCONCLUSIVE", () => {
    const runs = [...requiredPass(config), run("license", "ERROR"), run("strix", "FAIL")];
    expect(federate(runs, "LOW", config).status).toBe("FAIL");
  });

  it("optionalScanners: absence is not a coverage gap; execution still contributes", () => {
    expect(config.security.optionalScanners.length).toBeGreaterThan(0);
    expect(federate(requiredPass(config), "LOW", config).status).toBe("PASS");
    const optionalId = config.security.optionalScanners[0]!;
    expect(federate([...requiredPass(config), run(optionalId, "FAIL")], "LOW", config).status).toBe(
      "FAIL",
    );
  });
});
