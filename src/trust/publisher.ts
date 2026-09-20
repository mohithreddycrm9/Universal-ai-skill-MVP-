import type { TrustPolicy } from "../policy/load.js";
import type { SkillPackage, TrustTier } from "../types.js";
import { assertNever } from "../util/assert-never.js";

export interface TrustDecision {
  tier: TrustTier;
  evidence: string[];
  canScan: boolean;
  canApprove: boolean;
}

export function evaluatePublisher(pkg: SkillPackage, policy: TrustPolicy): TrustDecision {
  const login = pkg.publisher.trim().toLowerCase();
  const evidence: string[] = [`publisher_login=${pkg.publisher}`, `repository=${pkg.repository}`];

  if (policy.untrustedPublishers.map(lower).includes(login) || (policy.treatArchivedAsUntrusted && pkg.archived)) {
    if (pkg.archived) {
      evidence.push("repository_archived");
    }
    if (policy.untrustedPublishers.map(lower).includes(login)) {
      evidence.push("untrusted_allowlist_hit");
    }
    return { tier: "UNTRUSTED", evidence, canScan: false, canApprove: false };
  }

  if (policy.officialOrganizations.map(lower).includes(login)) {
    evidence.push("official_organization_allowlist");
    return { tier: "OFFICIAL", evidence, canScan: true, canApprove: true };
  }
  if (policy.verifiedPublishers.map(lower).includes(login)) {
    evidence.push("verified_publisher_allowlist");
    return { tier: "VERIFIED", evidence, canScan: true, canApprove: true };
  }
  if (policy.trustedCommunityPublishers.map(lower).includes(login)) {
    evidence.push("trusted_community_allowlist");
    return { tier: "TRUSTED_COMMUNITY", evidence, canScan: true, canApprove: true };
  }

  evidence.push("no_allowlist_match");
  evidence.push("name_similarity_is_not_evidence");
  return {
    tier: "UNKNOWN",
    evidence,
    canScan: policy.allowUnknownPublisherScan,
    canApprove: policy.allowUnknownPublisherApprove,
  };
}

export function describeTrustTier(tier: TrustTier): string {
  switch (tier) {
    case "OFFICIAL":
      return "Publisher matched a configured official organization allowlist plus policy checks.";
    case "VERIFIED":
      return "Publisher matched a configured verified-publisher allowlist.";
    case "TRUSTED_COMMUNITY":
      return "Publisher matched a configured trusted-community allowlist.";
    case "UNKNOWN":
      return "Publisher is UNKNOWN. UNKNOWN is not trusted.";
    case "UNTRUSTED":
      return "Publisher is UNTRUSTED based on negative evidence or policy.";
    default:
      return assertNever(tier, "Unknown trust tier");
  }
}

function lower(value: string): string {
  return value.trim().toLowerCase();
}
