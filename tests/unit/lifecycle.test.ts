import { describe, expect, it } from "vitest";
import { assertTransition, normalizeLifecycle } from "../../src/skills/lifecycle.js";
import { SkillMcpError } from "../../src/errors.js";

describe("lifecycle", () => {
  it("maps legacy persisted states onto the quarantine-first pipeline", () => {
    expect(normalizeLifecycle("UNTRUSTED")).toBe("QUARANTINED");
    expect(normalizeLifecycle("VERIFYING")).toBe("PROVENANCE_CHECK");
    expect(normalizeLifecycle("SCANNING")).toBe("SECURITY_SCAN");
    expect(normalizeLifecycle("SANDBOXING")).toBe("SANDBOX");
  });

  it("starts newly fetched content in quarantine and forbids skipping the gate", () => {
    expect(() => assertTransition("DISCOVERED", "QUARANTINED")).not.toThrow();
    expect(() => assertTransition("DISCOVERED", "AVAILABLE")).toThrow(SkillMcpError);
    expect(() => assertTransition("QUARANTINED", "AVAILABLE")).toThrow(SkillMcpError);
  });
});
