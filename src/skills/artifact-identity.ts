import { canonicalize } from "../util/canonical.js";
import { sha256 } from "../util/hash.js";
import { normalizeRepository } from "./fingerprint.js";
import type { CanonicalArtifactIdentity } from "../types.js";

export type { CanonicalArtifactIdentity };

export function buildArtifactIdentity(input: {
  sourceId: string;
  repository: string;
  resolvedCommitSha: string;
  contentFingerprint: string;
  filesDigest?: string;
}): CanonicalArtifactIdentity {
  const resolvedCommitSha = input.resolvedCommitSha.trim().toLowerCase();
  if (!resolvedCommitSha || isMutableRef(resolvedCommitSha)) {
    throw new Error("Canonical artifact identity requires an immutable commit SHA");
  }
  return {
    sourceId: input.sourceId.trim().toLowerCase(),
    repository: normalizeRepository(input.repository),
    resolvedCommitSha,
    contentFingerprint: input.contentFingerprint,
    ...(input.filesDigest ? { filesDigest: input.filesDigest } : {}),
  };
}

export function artifactIdentityKey(identity: CanonicalArtifactIdentity): string {
  return sha256(
    canonicalize({
      sourceId: identity.sourceId,
      repository: identity.repository,
      resolvedCommitSha: identity.resolvedCommitSha,
      contentFingerprint: identity.contentFingerprint,
      filesDigest: identity.filesDigest ?? "",
    }),
  );
}

export function commitsMatch(expected: string, actual: string): boolean {
  const a = expected.trim().toLowerCase();
  const b = actual.trim().toLowerCase();
  if (!a || !b) return false;
  return a === b || a.startsWith(b) || b.startsWith(a);
}

export function isMutableRef(value: string): boolean {
  const v = value.trim().toLowerCase();
  return !v || v === "latest" || v === "main" || v === "master" || v === "head" || v === "develop";
}

/** Prefer discovery-time pin; never silently swap A for later tip B. */
export function preferredAcquireRef(candidate: {
  resolvedCommitSha?: string;
  commit?: string;
  requestedRef?: string;
  defaultRef: string;
}): { ref: string; pinnedAtDiscover: boolean } {
  if (candidate.resolvedCommitSha && !isMutableRef(candidate.resolvedCommitSha)) {
    return { ref: candidate.resolvedCommitSha, pinnedAtDiscover: true };
  }
  if (candidate.commit && !isMutableRef(candidate.commit)) {
    return { ref: candidate.commit, pinnedAtDiscover: true };
  }
  return { ref: candidate.requestedRef ?? candidate.defaultRef, pinnedAtDiscover: false };
}
