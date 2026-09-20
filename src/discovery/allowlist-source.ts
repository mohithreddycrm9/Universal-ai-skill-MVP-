import type { SkillCandidate, SkillPackage } from "../types.js";
import { SkillMcpError } from "../errors.js";
import type { CostMetadata } from "../cost/types.js";
import type { DiscoveryQuery, PinnedRef, SkillRef, SkillSource } from "./skill-source.js";
import {
  type CatalogEntry,
  entryToCandidate,
  entryToPackage,
  findCatalogEntry,
  loadCatalogFile,
  matchCatalog,
} from "./catalog.js";
import { COST_CATALOG } from "../cost/catalog.js";

/** YAML allowlisted catalog. No vendor names are hard-coded in core. */
export class AllowlistedCatalogSource implements SkillSource {
  constructor(
    readonly id: string,
    readonly cost: CostMetadata,
    private readonly entries: CatalogEntry[],
  ) {}

  static fromFile(id: string, cost: CostMetadata, path: string): AllowlistedCatalogSource {
    return new AllowlistedCatalogSource(id, cost, loadCatalogFile(path));
  }

  async search(query: DiscoveryQuery): Promise<SkillCandidate[]> {
    return matchCatalog(this.entries, query.query, query.limit).map((entry) => entryToCandidate(entry, this.id));
  }

  async fetch(ref: SkillRef): Promise<SkillPackage> {
    const entry = findCatalogEntry(
      this.entries,
      ref.repositoryUrl,
      ref.owner && ref.repo ? `${ref.owner}/${ref.repo}` : undefined,
    );
    if (!entry) {
      throw new SkillMcpError("NOT_FOUND", `${this.id} allowlist has no package for ${ref.repositoryUrl}`);
    }
    return entryToPackage(entry, this.id);
  }

  async pin(ref: SkillRef): Promise<PinnedRef> {
    const pkg = await this.fetch(ref);
    return { repositoryUrl: pkg.repositoryUrl, commitSha: pkg.commitSha, ref: pkg.commitSha };
  }
}

export class OfficialVendorSource extends AllowlistedCatalogSource {
  constructor(entries: CatalogEntry[] = []) {
    super("official_vendor", COST_CATALOG.official_vendor_catalog, entries);
  }

  static fromCatalog(path: string): OfficialVendorSource {
    return new OfficialVendorSource(loadCatalogFile(path));
  }
}

export class EnterpriseRegistrySource extends AllowlistedCatalogSource {
  constructor(entries: CatalogEntry[] = []) {
    super("enterprise_registry", COST_CATALOG.enterprise_registry_catalog, entries);
  }

  static fromCatalog(path: string): EnterpriseRegistrySource {
    return new EnterpriseRegistrySource(loadCatalogFile(path));
  }
}
