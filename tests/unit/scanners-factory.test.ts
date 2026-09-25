import { describe, expect, it } from "vitest";
import { loadConfig } from "../../src/policy/load.js";
import { buildSecurityScanners } from "../../src/security/scanners-factory.js";

describe("buildSecurityScanners", () => {
  it("org default registers built-ins only", () => {
    const config = loadConfig("config");
    expect(config.security.extendedScanningEnabled).toBe(false);
    const ids = buildSecurityScanners(config, { now: () => new Date(0) }).map((s) => s.id);
    expect(ids).toEqual(["secret", "prompt_injection", "suspicious_files", "dependency", "license"]);
    expect(ids).not.toContain("skillspector");
  });

  it("extended mode adds OSS adapters", () => {
    const config = loadConfig("config");
    config.security.extendedScanningEnabled = true;
    const ids = buildSecurityScanners(config, { now: () => new Date(0) }).map((s) => s.id);
    expect(ids).toContain("skillspector");
    expect(ids).toContain("semgrep");
  });
});
