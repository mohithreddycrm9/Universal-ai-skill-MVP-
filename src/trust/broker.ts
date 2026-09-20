import type { TrustPolicy } from "../policy/load.js";
import type { SkillPackage, TrustTier } from "../types.js";
import { evaluatePublisher, type TrustDecision } from "./publisher.js";

export interface TrustProvider {
  evaluate(pkg: SkillPackage): TrustDecision;
}

export class AllowlistTrustProvider implements TrustProvider {
  constructor(private readonly policy: TrustPolicy) {}

  evaluate(pkg: SkillPackage): TrustDecision {
    return evaluatePublisher(pkg, this.policy);
  }
}

/**
 * Trust Broker: evidence in, tier out. Does not grant capabilities.
 * UNKNOWN is never upgraded to VERIFIED.
 */
export class TrustBroker {
  constructor(private readonly providers: TrustProvider[]) {}

  evaluate(pkg: SkillPackage): TrustDecision {
    const decisions = this.providers.map((provider) => provider.evaluate(pkg));
    return mergeTrust(decisions);
  }
}

export function mergeTrust(decisions: TrustDecision[]): TrustDecision {
  const rank: Record<TrustTier, number> = {
    UNTRUSTED: 0,
    UNKNOWN: 1,
    TRUSTED_COMMUNITY: 2,
    VERIFIED: 3,
    OFFICIAL: 4,
  };
  let best = decisions[0] ?? {
    tier: "UNKNOWN" as const,
    evidence: ["no_trust_provider"],
    canScan: false,
    canApprove: false,
  };
  for (const decision of decisions) {
    if (decision.tier === "UNTRUSTED") {
      return {
        ...decision,
        evidence: [...decision.evidence, "broker:untrusted_wins"],
        canApprove: false,
      };
    }
    if (rank[decision.tier] > rank[best.tier]) {
      best = decision;
    }
  }
  if (best.tier === "UNKNOWN") {
    return { ...best, evidence: [...best.evidence, "broker:unknown_is_not_verified"], canApprove: best.canApprove };
  }
  return best;
}
