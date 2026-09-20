export const PRICING_MODELS = ["free", "freemium", "paid", "usage_based", "unknown"] as const;
export type PricingModel = (typeof PRICING_MODELS)[number];

export const COST_POLICIES = [
  "ALLOW_FREE_ONLY",
  "ASK_BEFORE_ANY_PAID_OPERATION",
  "ALLOW_UP_TO_AMOUNT",
  "DENY_ALL_PAID_SERVICES",
] as const;
export type CostPolicyName = (typeof COST_POLICIES)[number];

export const COST_APPROVAL_STATES = ["PENDING", "APPROVED", "REJECTED"] as const;
export type CostApprovalState = (typeof COST_APPROVAL_STATES)[number];

export interface CostMetadata {
  provider: string;
  service: string;
  pricingModel: PricingModel;
  freeTier: boolean;
  estimatedCost: string;
  requiresApproval: boolean;
  purpose: string;
  freeAlternative?: string;
}

export interface CostPolicy {
  policy: CostPolicyName;
  currency: string;
  allowUpToAmount: number;
  preferFreeAlternatives: boolean;
  neverAutoPaidFallback: boolean;
  unknownCostRequiresApproval: boolean;
}

export interface CostReview {
  operation: string;
  provider: string;
  service: string;
  purpose: string;
  reason: string;
  potentialCost: string;
  freeAlternative?: string;
  risk: string;
  approvalRequired: true;
  notice: string;
}

export type CostDecision =
  | { kind: "FREE"; proceed: true; metadata: CostMetadata }
  | { kind: "APPROVED"; proceed: true; metadata: CostMetadata; approvalId: string }
  | { kind: "NEEDS_APPROVAL"; proceed: false; metadata: CostMetadata; review: CostReview }
  | { kind: "DENIED"; proceed: false; metadata: CostMetadata; reason: string };

export interface CostApproval {
  id: string;
  operation: string;
  provider: string;
  service: string;
  status: CostApprovalState;
  approver: string | null;
  review: CostReview;
  createdAt: string;
  updatedAt: string;
}

export function isClearlyFree(metadata: CostMetadata): boolean {
  if (metadata.requiresApproval) {
    return false;
  }
  if (metadata.pricingModel === "unknown" || metadata.pricingModel === "paid" || metadata.pricingModel === "usage_based") {
    return false;
  }
  if (!metadata.freeTier) {
    return false;
  }
  if (metadata.estimatedCost === "unknown") {
    return false;
  }
  return metadata.estimatedCost === "0" || metadata.estimatedCost.startsWith("0 ");
}

export function costIsUnknown(metadata: CostMetadata): boolean {
  return metadata.pricingModel === "unknown" || metadata.estimatedCost === "unknown";
}

export function isCommercial(metadata: CostMetadata): boolean {
  return metadata.pricingModel === "paid" || metadata.pricingModel === "usage_based" || metadata.requiresApproval;
}
