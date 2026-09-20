import { describe, expect, it } from "vitest";
import { computeFingerprint } from "../../src/skills/fingerprint.js";
import { canonicalize } from "../../src/util/canonical.js";

describe("fingerprint", () => {
  it("is deterministic for identical inputs regardless of key insertion order", () => {
    const a = computeFingerprint({
      publisher: "Acme",
      repository: "https://example.local/acme/skill.git",
      commitSha: "ABCDEF",
      manifestCanonical: canonicalize({ z: 1, a: 2 }),
      dependencyLockHash: "sha256:lock",
      securityConfigurationHash: "sha256:sec",
      filesDigest: "sha256:files",
    });
    const b = computeFingerprint({
      publisher: "acme",
      repository: "https://example.local/acme/skill",
      commitSha: "abcdef",
      manifestCanonical: canonicalize({ a: 2, z: 1 }),
      dependencyLockHash: "sha256:lock",
      securityConfigurationHash: "sha256:sec",
      filesDigest: "sha256:files",
    });
    expect(a).toBe(b);
    expect(a.startsWith("sha256:")).toBe(true);
  });

  it("changes when the commit changes", () => {
    const base = {
      publisher: "acme",
      repository: "https://example.local/acme/skill",
      manifestCanonical: "{}",
      dependencyLockHash: "sha256:lock",
      securityConfigurationHash: "sha256:sec",
    };
    const a = computeFingerprint({ ...base, commitSha: "aaa" });
    const b = computeFingerprint({ ...base, commitSha: "bbb" });
    expect(a).not.toBe(b);
  });
});
