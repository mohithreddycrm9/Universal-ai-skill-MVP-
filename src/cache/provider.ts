import type { SecurityStatus } from "../types.js";

export interface CacheEntry {
  fingerprint: string;
  skillId: string;
  securityStatus: SecurityStatus;
  scannerVersions: Record<string, string>;
  securityConfigHash: string;
  expiresAt: string;
}

export interface CacheProvider {
  get(fingerprint: string): CacheEntry | undefined;
  put(entry: CacheEntry): void;
}
