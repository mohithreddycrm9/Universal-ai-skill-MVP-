import { describe, expect, it } from "vitest";
import { CostDetector } from "../../src/cost/detector.js";
import { COST_CATALOG } from "../../src/cost/catalog.js";
import { isClearlyFree } from "../../src/cost/types.js";
import type { CostMetadata, CostPolicy } from "../../src/cost/types.js";

const ask: CostPolicy = {
  policy: "ASK_BEFORE_ANY_PAID_OPERATION",
  currency: "USD",
  allowUpToAmount: 0,
  preferFreeAlternatives: true,
  neverAutoPaidFallback: true,
  unknownCostRequiresApproval: true,
};

describe("cost detector", () => {
  it("lets clearly free operations proceed", () => {
    const detector = new CostDetector(ask);
    const decision = detector.evaluate("scan", COST_CATALOG.secret);
    expect(decision.kind).toBe("FREE");
    expect(decision.proceed).toBe(true);
    expect(isClearlyFree(COST_CATALOG.github_public)).toBe(true);
    expect(isClearlyFree(COST_CATALOG.mcp_registry_remote)).toBe(false);
    expect(isClearlyFree(COST_CATALOG.cloud_sandbox)).toBe(false);
  });

  it("requires approval for paid operations", () => {
    const detector = new CostDetector(ask);
    const decision = detector.evaluate("scan", COST_CATALOG.snyk);
    expect(decision.proceed).toBe(false);
    expect(decision.kind).toBe("NEEDS_APPROVAL");
    if (decision.kind === "NEEDS_APPROVAL") {
      expect(decision.review.notice).toMatch(/not required to approve/i);
      expect(decision.review.freeAlternative).toBeTruthy();
    }
  });

  it("requires approval when cost is unknown", () => {
    const detector = new CostDetector(ask);
    const unknown: CostMetadata = {
      provider: "Mystery",
      service: "Metered API",
      pricingModel: "unknown",
      freeTier: false,
      estimatedCost: "unknown",
      requiresApproval: true,
      purpose: "Unknown-cost call",
      freeAlternative: "LocalRegistrySource",
    };
    const decision = detector.evaluate("fetch", unknown);
    expect(decision.kind).toBe("NEEDS_APPROVAL");
    expect(decision.proceed).toBe(false);
  });

  it("stops after a human reject", () => {
    const detector = new CostDetector(ask);
    const decision = detector.evaluate("scan", COST_CATALOG.snyk, { approvalStatus: "REJECTED" });
    expect(decision.kind).toBe("DENIED");
    expect(decision.proceed).toBe(false);
  });

  it("denies paid work under ALLOW_FREE_ONLY", () => {
    const detector = new CostDetector({ ...ask, policy: "ALLOW_FREE_ONLY" });
    const decision = detector.evaluate("scan", COST_CATALOG.snyk);
    expect(decision.kind).toBe("DENIED");
  });
});

describe("SkillSpector LLM catalog", () => {
  it("denies skillspector_llm under ALLOW_FREE_ONLY", () => {
    const detector = new CostDetector({
      policy: "ALLOW_FREE_ONLY",
      currency: "USD",
      allowUpToAmount: 0,
      preferFreeAlternatives: true,
      neverAutoPaidFallback: true,
      unknownCostRequiresApproval: true,
    });
    expect(detector.evaluate("scan_skill:skillspector_llm", COST_CATALOG.skillspector_llm).proceed).toBe(false);
  });
});
