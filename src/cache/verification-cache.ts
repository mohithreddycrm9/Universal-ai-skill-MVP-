import type { SkillRegistry } from "../registry/skill-registry.js";
import type { CacheEntry, CacheProvider } from "./provider.js";
import type { Clock } from "../util/clock.js";

export class VerificationCache implements CacheProvider {
  constructor(
    private readonly registry: SkillRegistry,
    private readonly clock: Clock,
  ) {}

  get(fingerprint: string): CacheEntry | undefined {
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
    return hit;
  }

  put(entry: CacheEntry): void {
    if (entry.securityStatus !== "PASS") {
      return;
    }
    this.registry.putCache({ ...entry, createdAt: this.clock.now().toISOString() });
  }
}
