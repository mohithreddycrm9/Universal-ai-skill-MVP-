import { describe, expect, it } from "vitest";
import { TrustBroker, AllowlistTrustProvider, mergeTrust } from "../../src/trust/broker.js";
import { loadConfig } from "../../src/policy/load.js";
import { benignPackage } from "../helpers.js";
import { rankCandidates } from "../../src/discovery/rank.js";

describe("trust broker", () => {
  it("never treats UNKNOWN as VERIFIED and ignores vendor-shaped names", () => {
    const config = loadConfig("config");
    const broker = new TrustBroker([new AllowlistTrustProvider(config.trust)]);
    const pkg = {
      ...benignPackage(),
      publisher: "amazingly-official-cloud",
      ownerLogin: "amazingly-official-cloud",
      repository: "amazingly-official-cloud/aws-helper",
    };
    const decision = broker.evaluate(pkg);
    expect(decision.tier).toBe("UNKNOWN");
    expect(decision.evidence.join(" ")).toMatch(/name_similarity_is_not_evidence/);
  });

  it("does not upgrade UNKNOWN when merging", () => {
    const merged = mergeTrust([
      { tier: "UNKNOWN", evidence: ["a"], canScan: true, canApprove: false },
      { tier: "UNKNOWN", evidence: ["b"], canScan: true, canApprove: false },
    ]);
    expect(merged.tier).toBe("UNKNOWN");
  });

  it("ranks official sources ahead of unknown GitHub names", () => {
    const ranked = rankCandidates(
      [
        {
          candidateId: "g",
          sourceId: "github",
          name: "vendor-tool",
          description: "n",
          publisher: "random",
          repository: "random/vendor-tool",
          repositoryUrl: "https://example.local/r",
          defaultRef: "main",
        },
        {
          candidateId: "o",
          sourceId: "official_vendor",
          name: "tool",
          description: "n",
          publisher: "vendor",
          repository: "vendor/tool",
          repositoryUrl: "https://example.local/o",
          defaultRef: "v1",
        },
      ],
      () => "UNKNOWN",
    );
    expect(ranked[0]?.sourceId).toBe("official_vendor");
  });
});
