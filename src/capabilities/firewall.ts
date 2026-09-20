import type { Capability, FirewallDecision, RiskLevel } from "../types.js";
import { isCapability } from "../types.js";
import { SkillMcpError } from "../errors.js";
import type { TrustedApprovalDecision } from "../approvals/types.js";

const DANGEROUS: Capability[] = [
  "filesystem.write",
  "network.write",
  "shell.execute",
  "process.spawn",
  "credential.read",
  "cloud.write",
  "production.deploy",
];

export function isHighRiskCapability(capability: Capability): boolean {
  return DANGEROUS.includes(capability);
}

export class CapabilityFirewall {
  /**
   * Authorization uses a gateway-verified TrustedApprovalDecision only.
   * Raw MCP `approver` / `humanApproved` strings must never reach here as proof.
   */
  decide(
    capability: Capability,
    opts: { risk: RiskLevel; trustedApproval?: TrustedApprovalDecision },
  ): FirewallDecision {
    if (!isCapability(capability)) {
      return "DENY";
    }
    if (capability === "filesystem.read") {
      return "ALLOW";
    }
    const trusted =
      opts.trustedApproval?.method === "local_interactive" && Boolean(opts.trustedApproval.approvalId);
    if (DANGEROUS.includes(capability)) {
      if (trusted) {
        return opts.risk === "CRITICAL" && capability === "production.deploy"
          ? "REQUIRE_USER_APPROVAL"
          : "ALLOW";
      }
      return "DENY";
    }
    return trusted ? "ALLOW" : "REQUIRE_USER_APPROVAL";
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

  request(
    capability: string,
    opts: {
      risk: RiskLevel;
      current: Capability[];
      trustedApproval?: TrustedApprovalDecision;
    },
  ): Capability[] {
    if (!isCapability(capability)) {
      throw new SkillMcpError("INVALID_INPUT", `Unknown capability ${capability}`);
    }
    const decision = this.decide(capability, opts);
    if (decision === "DENY") {
      throw new SkillMcpError(
        "POLICY_DENIED",
        `Capability ${capability} denied. Skills cannot self-grant permissions. Use CLI skill-mcp approve <id> after request_capability creates a PENDING record.`,
        {
          capability,
          decision,
        },
      );
    }
    if (decision === "REQUIRE_USER_APPROVAL" && !opts.trustedApproval) {
      throw new SkillMcpError("POLICY_DENIED", `Capability ${capability} requires user approval via local interactive CLI`, {
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
