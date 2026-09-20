import type { SkillCandidate, SkillPackage } from "../types.js";
import type { CostMetadata } from "../cost/types.js";

export interface DiscoveryQuery {
  query: string;
  domain?: string;
  limit: number;
}

export interface SkillRef {
  repositoryUrl: string;
  ref?: string;
  owner?: string;
  repo?: string;
}

export interface PinnedRef {
  repositoryUrl: string;
  commitSha: string;
  ref: string;
}

export interface SkillSource {
  readonly id: string;
  readonly cost: CostMetadata;
  search(query: DiscoveryQuery): Promise<SkillCandidate[]>;
  fetch(ref: SkillRef): Promise<SkillPackage>;
  pin(ref: SkillRef): Promise<PinnedRef>;
}
