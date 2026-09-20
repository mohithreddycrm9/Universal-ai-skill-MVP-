import type { CanonicalLifecycle, Lifecycle } from "../types.js";
import { SkillMcpError } from "../errors.js";
import { assertNever } from "../util/assert-never.js";

const LEGACY: Record<string, CanonicalLifecycle> = {
  UNTRUSTED: "QUARANTINED",
  VERIFYING: "PROVENANCE_CHECK",
  SCANNING: "SECURITY_SCAN",
  SANDBOXING: "SANDBOX",
};

export function normalizeLifecycle(state: Lifecycle): CanonicalLifecycle {
  const mapped = LEGACY[state];
  if (mapped) {
    return mapped;
  }
  return state as CanonicalLifecycle;
}

const ALLOWED: Record<CanonicalLifecycle, readonly CanonicalLifecycle[]> = {
  DISCOVERED: ["QUARANTINED", "REJECTED", "INVALIDATED"],
  QUARANTINED: ["PROVENANCE_CHECK", "REJECTED", "INVALIDATED"],
  PROVENANCE_CHECK: ["SECURITY_SCAN", "QUARANTINED", "REJECTED", "INVALIDATED"],
  SECURITY_SCAN: ["SANDBOX", "POLICY_EVALUATION", "QUARANTINED", "REJECTED", "INVALIDATED"],
  SANDBOX: ["POLICY_EVALUATION", "QUARANTINED", "REJECTED", "INVALIDATED"],
  POLICY_EVALUATION: ["APPROVED", "AVAILABLE", "QUARANTINED", "REJECTED", "INVALIDATED"],
  APPROVED: ["AVAILABLE", "EXPIRED", "INVALIDATED", "QUARANTINED", "REJECTED"],
  AVAILABLE: ["EXPIRED", "INVALIDATED", "QUARANTINED", "REJECTED"],
  REJECTED: ["INVALIDATED"],
  EXPIRED: ["PROVENANCE_CHECK", "INVALIDATED", "REJECTED"],
  INVALIDATED: [],
};

export function assertTransition(from: Lifecycle, to: Lifecycle): void {
  if (from === to) {
    return;
  }
  const a = normalizeLifecycle(from);
  const b = normalizeLifecycle(to);
  if (a === b) {
    return;
  }
  if (!ALLOWED[a].includes(b)) {
    throw new SkillMcpError("ILLEGAL_LIFECYCLE", `Illegal lifecycle transition ${from} → ${to}`, { from, to });
  }
}

export function describeLifecycle(state: Lifecycle): string {
  const canonical = normalizeLifecycle(state);
  switch (canonical) {
    case "DISCOVERED":
      return "Candidate recorded; not trusted and not authorized.";
    case "QUARANTINED":
      return "Held in quarantine. Newly fetched content starts here; findings may return it here.";
    case "PROVENANCE_CHECK":
      return "Publisher provenance and immutable commit pin in progress.";
    case "SECURITY_SCAN":
      return "Configured scanners running or recorded.";
    case "SANDBOX":
      return "Executable components under sandbox observation.";
    case "POLICY_EVALUATION":
      return "Authorization (capability firewall) independent of trust.";
    case "APPROVED":
      return "Configured security controls passed for this fingerprint. Not a universal safety claim. May still be unauthorized.";
    case "AVAILABLE":
      return "Approved and authorized to serve a compact skill card (progressive disclosure).";
    case "REJECTED":
      return "Not approved under current policy.";
    case "EXPIRED":
      return "Verification expired; refresh required. Treat as a new artifact if the commit moved.";
    case "INVALIDATED":
      return "Operator invalidated this skill version.";
    default:
      return assertNever(canonical, "Unknown lifecycle");
  }
}
