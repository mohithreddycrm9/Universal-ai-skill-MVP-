import type { Capability, FirewallDecision, RiskLevel } from "../types.js";
import { isCapability } from "../types.js";
import { SkillMcpError } from "../errors.js";

const DANGEROUS: Capability[] = [
  "filesystem.write",
  "network.write",
  "shell.execute",
  "process.spawn",
  "credential.read",
  "cloud.write",
  "production.deploy",
];

export class CapabilityFirewall {
  decide(capability: Capability, opts: { approver?: string; risk: RiskLevel }): FirewallDecision {
    if (!isCapability(capability)) {
      return "DENY";
    }
    if (capability === "filesystem.read") {
      return "ALLOW";
    }
    if (DANGEROUS.includes(capability)) {
      if (opts.approver) {
        return opts.risk === "CRITICAL" && capability === "production.deploy" ? "REQUIRE_USER_APPROVAL" : "ALLOW";
      }
      return "DENY";
    }
    return opts.approver ? "ALLOW" : "REQUIRE_USER_APPROVAL";
  }

  /**
   * Effective permissions are firewall-owned. Manifest-declared extras are ignored.
   */
  effective(declared: Capability[], granted: Capability[]): Capability[] {
    const allowed = new Set<Capability>(granted);
    allowed.add("filesystem.read");
    for (const item of declared) {
      if (!granted.includes(item) && item !== "filesystem.read") {
        continue;
      }
    }
    return [...allowed];
  }

  request(capability: string, opts: { approver?: string; risk: RiskLevel; current: Capability[] }): Capability[] {
    if (!isCapability(capability)) {
      throw new SkillMcpError("INVALID_INPUT", `Unknown capability ${capability}`);
    }
    const decision = this.decide(capability, opts);
    if (decision === "DENY") {
      throw new SkillMcpError("POLICY_DENIED", `Capability ${capability} denied. Skills cannot self-grant permissions.`, {
        capability,
        decision,
      });
    }
    if (decision === "REQUIRE_USER_APPROVAL" && !opts.approver) {
      throw new SkillMcpError("POLICY_DENIED", `Capability ${capability} requires user approval`, {
        capability,
        decision,
      });
    }
    if (opts.current.includes(capability)) {
      return opts.current;
    }
    return [...opts.current, capability];
  }
}
