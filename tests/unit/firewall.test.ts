import { describe, expect, it } from "vitest";
import { CapabilityFirewall } from "../../src/capabilities/firewall.js";
import { SkillMcpError } from "../../src/errors.js";

describe("capability firewall", () => {
  const fw = new CapabilityFirewall();

  it("effective = declared ∩ granted (declared+granted kept)", () => {
    const effective = fw.effective(
      ["filesystem.read", "network.read"],
      ["filesystem.read", "network.read", "shell.execute"],
    );
    expect(effective).toEqual(expect.arrayContaining(["filesystem.read", "network.read"]));
    expect(effective).not.toContain("shell.execute");
  });

  it("declared-not-granted is excluded (no self-grant)", () => {
    const effective = fw.effective(["filesystem.read", "shell.execute"], ["filesystem.read"]);
    expect(effective).toEqual(["filesystem.read"]);
  });

  it("granted-not-declared is excluded unless baseline policy", () => {
    const effective = fw.effective(["filesystem.read"], ["filesystem.read", "network.read"]);
    expect(effective).toEqual(["filesystem.read"]);
    expect(effective).not.toContain("network.read");
  });

  it("baseline filesystem.read is always present", () => {
    const effective = fw.effective([], []);
    expect(effective).toEqual(["filesystem.read"]);
  });

  it("elevated capability needs an explicit grant path (approver)", () => {
    expect(() => fw.request("shell.execute", { risk: "HIGH", current: ["filesystem.read"] })).toThrow(
      SkillMcpError,
    );
    const next = fw.request("shell.execute", {
      risk: "HIGH",
      current: ["filesystem.read"],
      approver: "human",
    });
    expect(next).toContain("shell.execute");
  });
});
