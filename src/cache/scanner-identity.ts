/**
 * Identity of a scanner implementation that materially participates in
 * verification. Used for verification-cache reuse: same artifact + same
 * security policy + same material scanner implementations → cache MAY hit.
 */
export interface ScannerImplementationIdentity {
  name: string;
  version: string;
}

/** Versions that cannot authorize cache reuse (fail closed). */
export function isKnownScannerVersion(version: string | undefined | null): boolean {
  if (typeof version !== "string") {
    return false;
  }
  const trimmed = version.trim();
  if (!trimmed) {
    return false;
  }
  return trimmed.toLowerCase() !== "unknown";
}

/**
 * Enabled scanners from the runtime registry (not whole app deps).
 * Matches SecurityOrchestrator enablement: missing config ⇒ enabled.
 */
export function collectMaterialScannerImplementations(
  scanners: ReadonlyArray<{ id: string; version: string }>,
  scannerEnabled: Record<string, { enabled?: boolean } | undefined>,
): ScannerImplementationIdentity[] {
  const material: ScannerImplementationIdentity[] = [];
  for (const scanner of scanners) {
    const cfg = scannerEnabled[scanner.id];
    if (cfg?.enabled === false) {
      continue;
    }
    material.push({ name: scanner.id, version: scanner.version });
  }
  return normalizeScannerImplementationIdentities(material);
}

/** Deterministic sort by name, then version (order A,B === B,A). */
export function normalizeScannerImplementationIdentities(
  identities: readonly ScannerImplementationIdentity[],
): ScannerImplementationIdentity[] {
  return identities
    .map((item) => ({ name: item.name.trim(), version: item.version.trim() }))
    .filter((item) => item.name.length > 0)
    .sort((a, b) => {
      const byName = a.name.localeCompare(b.name);
      return byName !== 0 ? byName : a.version.localeCompare(b.version);
    });
}

export function materialScannerVersionsRecord(
  identities: readonly ScannerImplementationIdentity[],
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const item of normalizeScannerImplementationIdentities(identities)) {
    out[item.name] = item.version;
  }
  return out;
}

/** True only when every material scanner has a known (non-empty, non-"unknown") version. */
export function materialScannersAllowCacheReuse(
  identities: readonly ScannerImplementationIdentity[],
): boolean {
  const normalized = normalizeScannerImplementationIdentities(identities);
  if (normalized.length === 0) {
    return true;
  }
  return normalized.every((item) => isKnownScannerVersion(item.version));
}

/**
 * Legacy / incomplete entries lack usable scanner identity → treat as miss.
 * Empty object or any unknown/blank version ⇒ not reusable.
 */
export function cacheEntryHasScannerIdentity(
  scannerVersions: Record<string, string> | null | undefined,
): boolean {
  if (!scannerVersions || typeof scannerVersions !== "object") {
    return false;
  }
  const keys = Object.keys(scannerVersions);
  if (keys.length === 0) {
    return false;
  }
  return keys.every((name) => isKnownScannerVersion(scannerVersions[name]));
}

/** Exact set equality of name→version (add/remove/upgrade/downgrade ⇒ mismatch). */
export function scannerVersionSetsEqual(
  stored: Record<string, string>,
  expected: Record<string, string>,
): boolean {
  const storedKeys = Object.keys(stored).sort();
  const expectedKeys = Object.keys(expected).sort();
  if (storedKeys.length !== expectedKeys.length) {
    return false;
  }
  for (let i = 0; i < storedKeys.length; i++) {
    const key = storedKeys[i]!;
    if (key !== expectedKeys[i]) {
      return false;
    }
    if (stored[key] !== expected[key]) {
      return false;
    }
  }
  return true;
}
