import { describe, expect, it } from "vitest";
import { CapabilityFirewall } from "../../src/capabilities/firewall.js";
import { SkillMcpError } from "../../src/errors.js";

describe("capability firewall", () => {
  it("ignores self-grant attempts and denies shell without an approver", () => {
    const fw = new CapabilityFirewall();
    const effective = fw.effective(["filesystem.read", "shell.execute"], ["filesystem.read"]);
    expect(effective).toEqual(["filesystem.read"]);
    expect(() => fw.request("shell.execute", { risk: "HIGH", current: ["filesystem.read"] })).toThrow(SkillMcpError);
  });
});
