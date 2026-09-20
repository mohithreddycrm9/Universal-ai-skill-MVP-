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
   * Effective permissions = declared ∩ granted (+ filesystem.read baseline). Skills cannot self-grant.
   */
  effective(declared: Capability[], granted: Capability[]): Capability[] {
    // effective = declared ∩ granted, plus explicit baseline (filesystem.read).
    // Skills cannot self-grant: declared-but-not-granted is excluded.
    // Granted-but-not-declared is excluded unless it is the baseline policy grant.
    const grantedSet = new Set(granted);
    const result = new Set<Capability>();
    result.add("filesystem.read");
    for (const item of declared) {
      if (grantedSet.has(item)) {
        result.add(item);
      }
    }
    return [...result];
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
