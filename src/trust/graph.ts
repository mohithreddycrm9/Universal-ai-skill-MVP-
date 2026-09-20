import type { SkillRegistry } from "../registry/skill-registry.js";
import { newId } from "../util/ids.js";
import type { Clock } from "../util/clock.js";
import { iso } from "../util/clock.js";
import type { TrustDecision } from "./publisher.js";
import type { SkillPackage } from "../types.js";

export class TrustGraph {
  constructor(
    private readonly registry: SkillRegistry,
    private readonly clock: Clock,
  ) {}

  recordPackage(pkg: SkillPackage, decision: TrustDecision, fingerprint: string): void {
    const publisher = `publisher:${pkg.publisher}`;
    const org = `organization:${pkg.ownerLogin}`;
    const repo = `repository:${pkg.repository}`;
    const commit = `commit:${pkg.commitSha}`;
    const skill = `skill:${fingerprint}`;
    this.edge(publisher, org, "publishes_as", { login: pkg.publisher });
    this.edge(org, repo, "owns", { repository: pkg.repository, archived: pkg.archived });
    this.edge(repo, commit, "pins", { commitSha: pkg.commitSha });
    this.edge(commit, skill, "materializes", { fingerprint, tier: decision.tier, evidence: decision.evidence });
  }

  private edge(fromNode: string, toNode: string, kind: string, evidence: unknown): void {
    this.registry.insertTrustEdge({
      id: newId("trg"),
      fromNode,
      toNode,
      kind,
      evidence,
      createdAt: iso(this.clock),
    });
  }
}
