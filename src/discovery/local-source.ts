import type { SkillCandidate, SkillPackage } from "../types.js";
import { SkillMcpError } from "../errors.js";
import type { DiscoveryQuery, PinnedRef, SkillRef, SkillSource } from "./skill-source.js";
import { COST_CATALOG } from "../cost/catalog.js";

/**
 * Local / test SkillSource. GitHub remains the first remote adapter; this exists so
 * acquisition can be tested without network and so example skills can be loaded.
 */
export class LocalSource implements SkillSource {
  readonly id = "local";
  readonly cost = COST_CATALOG.local_source;
  private readonly packages = new Map<string, SkillPackage>();

  register(pkg: SkillPackage): void {
    this.packages.set(normalize(pkg.repositoryUrl), pkg);
    this.packages.set(normalize(pkg.repository), pkg);
  }

  async search(query: DiscoveryQuery): Promise<SkillCandidate[]> {
    const q = query.query.toLowerCase();
    const hits: SkillCandidate[] = [];
    const seen = new Set<string>();
    for (const pkg of this.packages.values()) {
      if (seen.has(pkg.repositoryUrl)) {
        continue;
      }
      seen.add(pkg.repositoryUrl);
      const blob = `${pkg.repository} ${pkg.publisher} ${pkg.files.map((f) => f.content).join("\n")}`.toLowerCase();
      if (!blob.includes(q) && !pkg.repository.toLowerCase().includes(q)) {
        continue;
      }
      hits.push({
        candidateId: `local:${pkg.repository}`,
        sourceId: this.id,
        name: pkg.repository.split("/").pop() ?? pkg.repository,
        description: pkg.files.find((f) => /skill\.ya?ml$/i.test(f.path))?.content.slice(0, 200) ?? pkg.repository,
        publisher: pkg.publisher,
        repository: pkg.repository,
        repositoryUrl: pkg.repositoryUrl,
        defaultRef: pkg.commitSha,
      });
      if (hits.length >= query.limit) {
        break;
      }
    }
    return hits;
  }

  async fetch(ref: SkillRef): Promise<SkillPackage> {
    const pkg = this.packages.get(normalize(ref.repositoryUrl)) ?? this.packages.get(normalize(ref.owner && ref.repo ? `${ref.owner}/${ref.repo}` : ""));
    if (!pkg) {
      throw new SkillMcpError("NOT_FOUND", `Local skill package not found for ${ref.repositoryUrl}`);
    }
    return pkg;
  }

  async pin(ref: SkillRef): Promise<PinnedRef> {
    const pkg = await this.fetch(ref);
    if (!pkg.commitSha || pkg.commitSha === "latest" || pkg.commitSha === "main") {
      throw new SkillMcpError("SOURCE_ERROR", "Refusing to pin a floating ref; commit SHA required");
    }
    return { repositoryUrl: pkg.repositoryUrl, commitSha: pkg.commitSha, ref: pkg.commitSha };
  }
}

function normalize(value: string): string {
  return value.trim().toLowerCase().replace(/\.git$/, "");
}
