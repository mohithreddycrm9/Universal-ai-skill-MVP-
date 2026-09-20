import { describe, expect, it } from "vitest";
import {
  artifactIdentityKey,
  buildArtifactIdentity,
  commitsMatch,
  preferredAcquireRef,
} from "../../src/skills/artifact-identity.js";

describe("canonical artifact identity", () => {
  it("keys by source+repo+commit+content fingerprint (+filesDigest)", () => {
    const a = buildArtifactIdentity({
      sourceId: "github",
      repository: "https://example.local/acme/skill.git",
      resolvedCommitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      contentFingerprint: "sha256:content-a",
      filesDigest: "sha256:files-a",
    });
    const b = buildArtifactIdentity({
      sourceId: "github",
      repository: "https://example.local/acme/skill",
      resolvedCommitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      contentFingerprint: "sha256:content-a",
      filesDigest: "sha256:files-a",
    });
    expect(artifactIdentityKey(a)).toBe(artifactIdentityKey(b));
    const c = buildArtifactIdentity({
      ...a,
      resolvedCommitSha: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
    });
    expect(artifactIdentityKey(a)).not.toBe(artifactIdentityKey(c));
  });

  it("rejects mutable refs as identity", () => {
    expect(() =>
      buildArtifactIdentity({
        sourceId: "github",
        repository: "acme/skill",
        resolvedCommitSha: "main",
        contentFingerprint: "sha256:x",
      }),
    ).toThrow(/immutable commit/i);
  });

  it("preferredAcquireRef prefers discovery pin", () => {
    expect(
      preferredAcquireRef({
        defaultRef: "main",
        requestedRef: "main",
        resolvedCommitSha: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      }),
    ).toEqual({ ref: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", pinnedAtDiscover: true });
    expect(preferredAcquireRef({ defaultRef: "main", requestedRef: "main" })).toEqual({
      ref: "main",
      pinnedAtDiscover: false,
    });
  });

  it("commitsMatch allows prefix equality", () => {
    expect(commitsMatch("abcdef", "abcdef0123")).toBe(true);
    expect(commitsMatch("aaaa", "bbbb")).toBe(false);
  });
});
