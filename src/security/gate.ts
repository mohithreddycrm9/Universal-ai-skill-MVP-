import type { FederatedSecurityResult } from "./orchestrator.js";
import type { SecurityPolicy } from "../policy/load.js";
import { SkillMcpError } from "../errors.js";

export function assertSecurityGate(result: FederatedSecurityResult, policy: SecurityPolicy): void {
  if (result.status === "FAIL" && policy.denyApproveOnFail) {
    throw new SkillMcpError("SECURITY_GATE", "Security gate FAILED; approval denied", { claim: result.claim });
  }
  if (result.status === "INCONCLUSIVE" && policy.denyApproveOnInconclusive) {
    throw new SkillMcpError(
      "SECURITY_GATE",
      "Security gate INCONCLUSIVE; INCONCLUSIVE is not PASS and approval is denied",
      { claim: result.claim, requiredMissing: result.requiredMissing },
    );
  }
  if (result.status !== "PASS") {
    throw new SkillMcpError("SECURITY_GATE", `Security gate status ${result.status} is not PASS`, {
      claim: result.claim,
    });
  }
}
