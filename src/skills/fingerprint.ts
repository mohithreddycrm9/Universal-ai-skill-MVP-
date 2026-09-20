import { canonicalize } from "../util/canonical.js";
import { sha256 } from "../util/hash.js";
import type { FingerprintInput } from "../types.js";

export function computeFingerprint(input: FingerprintInput): string {
  return sha256(
    canonicalize({
      publisher: input.publisher.trim().toLowerCase(),
      repository: normalizeRepository(input.repository),
      commitSha: input.commitSha.trim().toLowerCase(),
      manifestCanonical: input.manifestCanonical,
      dependencyLockHash: input.dependencyLockHash,
      securityConfigurationHash: input.securityConfigurationHash,
      filesDigest: input.filesDigest ?? "",
    }),
  );
}

export function normalizeRepository(repository: string): string {
  return repository
    .trim()
    .replace(/\.git$/i, "")
    .replace(/\/+$/, "")
    .toLowerCase();
}
