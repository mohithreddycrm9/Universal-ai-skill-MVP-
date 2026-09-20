import type { SkillRegistry } from "../registry/skill-registry.js";
import type { CacheEntry, CacheProvider } from "./provider.js";
import type { Clock } from "../util/clock.js";
import {
  cacheEntryHasScannerIdentity,
  materialScannersAllowCacheReuse,
  materialScannerVersionsRecord,
  scannerVersionSetsEqual,
  type ScannerImplementationIdentity,
} from "./scanner-identity.js";

export interface VerificationCacheOptions {
  /**
   * Material scanner implementations currently configured/enabled.
   * Cache reuse requires an exact match against the stored entry.
   */
  materialScanners?: readonly ScannerImplementationIdentity[];
}

export class VerificationCache implements CacheProvider {
  private readonly materialVersions: Record<string, string>;
  private readonly reuseAllowed: boolean;

  constructor(
    private readonly registry: SkillRegistry,
    private readonly clock: Clock,
    opts: VerificationCacheOptions = {},
  ) {
    const material = opts.materialScanners ?? [];
    this.materialVersions = materialScannerVersionsRecord(material);
    this.reuseAllowed = materialScannersAllowCacheReuse(material);
  }

  get(fingerprint: string): CacheEntry | undefined {
    // Unknown scanner version → deny cache reuse (fail closed).
    if (!this.reuseAllowed) {
      return undefined;
    }
    const hit = this.registry.getCache(fingerprint);
    if (!hit) {
      return undefined;
    }
    if (new Date(hit.expiresAt).getTime() <= this.clock.now().getTime()) {
      return undefined;
    }
    if (hit.securityStatus !== "PASS") {
      return undefined;
    }
    // Legacy entries lacking scanner identity → miss and reverify.
    if (!cacheEntryHasScannerIdentity(hit.scannerVersions)) {
      return undefined;
    }
    // Upgrade / downgrade / add / remove of material scanners → miss.
    if (!scannerVersionSetsEqual(hit.scannerVersions, this.materialVersions)) {
      return undefined;
    }
    return hit;
  }

  put(entry: CacheEntry): void {
    if (entry.securityStatus !== "PASS") {
      return;
    }
    this.registry.putCache({ ...entry, createdAt: this.clock.now().toISOString() });
  }
}
