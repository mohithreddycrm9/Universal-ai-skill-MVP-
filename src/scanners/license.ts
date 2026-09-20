import type { Clock } from "../util/clock.js";
import { systemClock } from "../util/clock.js";
import type { ScanTarget, ScannerRun } from "../types.js";
import type { ScannerConfig, SecurityScanner } from "./types.js";
import { finding, runEnvelope, walkTarget } from "./helpers.js";
import { COST_CATALOG } from "../cost/catalog.js";

export class LicenseScanner implements SecurityScanner {
  readonly id = "license";
  readonly version = "1.0.0";
  readonly cost = COST_CATALOG.license;

  constructor(private readonly clock: Clock = systemClock) {}

  async scan(target: ScanTarget, configuration: ScannerConfig): Promise<ScannerRun> {
    const allowed = Array.isArray(configuration.allowed)
      ? (configuration.allowed as string[])
      : ["MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "Unlicense", "0BSD"];
    const licenseFile = walkTarget(target).find((file) => /(^|\/)LICEN[CS]E/i.test(file.path));
    const spdx = target.package.license;
    if (!spdx && !licenseFile) {
      return runEnvelope(this.id, this.version, this.clock, "INCONCLUSIVE", [
        finding("lic:missing", "LOW", "No license file or SPDX id", undefined, "license unknown"),
      ], "License not determined; INCONCLUSIVE is not PASS");
    }
    const id = spdx ?? guessLicense(licenseFile?.content ?? "");
    if (id && !allowed.includes(id)) {
      return runEnvelope(this.id, this.version, this.clock, "FAIL", [
        finding("lic:policy", "MEDIUM", `License ${id} not in policy allowlist`, licenseFile?.path, id),
      ]);
    }
    return runEnvelope(this.id, this.version, this.clock, "PASS", [], `license=${id ?? "detected"}`);
  }
}

function guessLicense(text: string): string | undefined {
  if (/MIT License/i.test(text)) {
    return "MIT";
  }
  if (/Apache License/i.test(text)) {
    return "Apache-2.0";
  }
  return undefined;
}
