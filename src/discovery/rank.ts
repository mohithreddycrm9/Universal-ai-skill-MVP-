import type { SkillCandidate, TrustTier } from "../types.js";

const SOURCE_RANK: Record<string, number> = {
  official_vendor: 0,
  official_docs: 1,
  mcp_registry: 2,
  enterprise_registry: 3,
  github: 4,
  local: 4,
  unknown: 8,
};

const TIER_RANK: Record<TrustTier, number> = {
  OFFICIAL: 0,
  VERIFIED: 1,
  TRUSTED_COMMUNITY: 2,
  UNKNOWN: 5,
  UNTRUSTED: 9,
};

/**
 * Official-first ordering. A name that merely contains a vendor string does not improve rank.
 */
export function rankCandidates(
  candidates: SkillCandidate[],
  trustOf: (candidate: SkillCandidate) => TrustTier,
): SkillCandidate[] {
  return [...candidates].sort((a, b) => {
    const sa = SOURCE_RANK[a.sourceId] ?? 7;
    const sb = SOURCE_RANK[b.sourceId] ?? 7;
    if (sa !== sb) {
      return sa - sb;
    }
    const ta = TIER_RANK[trustOf(a)] ?? 5;
    const tb = TIER_RANK[trustOf(b)] ?? 5;
    if (ta !== tb) {
      return ta - tb;
    }
    return a.name.localeCompare(b.name);
  });
}
