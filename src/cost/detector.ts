import { SkillMcpError } from "../errors.js";
import { assertNever } from "../util/assert-never.js";
import type { CostDecision, CostMetadata, CostPolicy, CostReview } from "./types.js";
import { costIsUnknown, isClearlyFree } from "./types.js";

export function parseEstimatedAmount(estimatedCost: string): number | undefined {
  if (estimatedCost === "unknown") {
    return undefined;
  }
  const match = estimatedCost.match(/([0-9]+(?:\.[0-9]+)?)/);
  if (!match?.[1]) {
    return undefined;
  }
  return Number(match[1]);
}

export function buildCostReview(operation: string, metadata: CostMetadata, reason: string): CostReview {
  return {
    operation,
    provider: metadata.provider,
    service: metadata.service,
    purpose: metadata.purpose,
    reason,
    potentialCost: `${metadata.estimatedCost}${metadata.pricingModel === "unknown" || metadata.estimatedCost === "unknown" ? " (exact cost unknown — not assumed free)" : ""}`,
    freeAlternative: metadata.freeAlternative,
    risk: "May incur a charge, subscription, or metered usage. Rejecting leaves the operation unperformed.",
    approvalRequired: true,
    notice:
      "Explicit human approval is required. You are not required to approve. There is no penalty for rejecting. The MCP will not proceed silently.",
  };
}

export class CostDetector {
  constructor(private readonly policy: CostPolicy) {}

  evaluate(
    operation: string,
    metadata: CostMetadata,
    opts: { approvalStatus?: "APPROVED" | "REJECTED"; approvalId?: string } = {},
  ): CostDecision {
    if (opts.approvalStatus === "REJECTED") {
      return {
        kind: "DENIED",
        proceed: false,
        metadata,
        reason: "Human rejected this potentially billable operation. It will not run.",
      };
    }
    if (opts.approvalStatus === "APPROVED" && opts.approvalId) {
      return { kind: "APPROVED", proceed: true, metadata, approvalId: opts.approvalId };
    }

    const free = isClearlyFree(metadata);
    const unknown = costIsUnknown(metadata);

    switch (this.policy.policy) {
      case "ALLOW_FREE_ONLY":
      case "DENY_ALL_PAID_SERVICES":
        if (free) {
          return { kind: "FREE", proceed: true, metadata };
        }
        return {
          kind: "DENIED",
          proceed: false,
          metadata,
          reason:
            this.policy.policy === "ALLOW_FREE_ONLY"
              ? "Policy ALLOW_FREE_ONLY forbids paid/unknown-cost operations."
              : "Policy DENY_ALL_PAID_SERVICES forbids this operation.",
        };
      case "ASK_BEFORE_ANY_PAID_OPERATION":
        if (free) {
          return { kind: "FREE", proceed: true, metadata };
        }
        return {
          kind: "NEEDS_APPROVAL",
          proceed: false,
          metadata,
          review: buildCostReview(
            operation,
            metadata,
            unknown
              ? "Cost is unknown. Unknown cost is not treated as free."
              : "This operation may incur a charge.",
          ),
        };
      case "ALLOW_UP_TO_AMOUNT": {
        if (free) {
          return { kind: "FREE", proceed: true, metadata };
        }
        if (unknown || metadata.estimatedCost === "unknown") {
          return {
            kind: "NEEDS_APPROVAL",
            proceed: false,
            metadata,
            review: buildCostReview(
              operation,
              metadata,
              "Exact cost is unknown, so ALLOW_UP_TO_AMOUNT cannot auto-allow it.",
            ),
          };
        }
        const amount = parseEstimatedAmount(metadata.estimatedCost);
        if (amount !== undefined && amount <= this.policy.allowUpToAmount) {
          return { kind: "FREE", proceed: true, metadata };
        }
        return {
          kind: "NEEDS_APPROVAL",
          proceed: false,
          metadata,
          review: buildCostReview(
            operation,
            metadata,
            `Estimated cost exceeds allowUpToAmount (${this.policy.allowUpToAmount} ${this.policy.currency}).`,
          ),
        };
      }
      default:
        return assertNever(this.policy.policy, "cost policy");
    }
  }

  assertProceed(decision: CostDecision): void {
    if (decision.proceed) {
      return;
    }
    if (decision.kind === "NEEDS_APPROVAL") {
      throw new SkillMcpError("COST_APPROVAL_REQUIRED", "Potentially billable operation requires explicit human approval", {
        review: decision.review,
      });
    }
    throw new SkillMcpError("POLICY_DENIED", decision.reason, { metadata: decision.metadata });
  }
}
