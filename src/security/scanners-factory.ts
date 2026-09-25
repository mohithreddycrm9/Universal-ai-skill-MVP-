import type { Clock } from "../util/clock.js";
import type { AppConfig } from "../policy/load.js";
import { COST_CATALOG } from "../cost/catalog.js";
import { DependencyScanner } from "../scanners/dependency.js";
import { LicenseScanner } from "../scanners/license.js";
import { OssBinaryScanner } from "../scanners/oss-binary.js";
import { PromptInjectionScanner } from "../scanners/prompt-injection.js";
import { SecretScanner } from "../scanners/secret.js";
import { SkillspectorScanner } from "../scanners/skillspector.js";
import { SuspiciousFilesScanner } from "../scanners/suspicious-files.js";
import type { SecurityScanner } from "../scanners/types.js";

/**
 * Org-approved default: built-in static scanners only (no external CLIs, no commercial, no LLM).
 * Set `extendedScanningEnabled: true` in security-policy to register optional OSS adapters.
 */
export function buildSecurityScanners(config: AppConfig, clock: Clock): SecurityScanner[] {
  const scanners: SecurityScanner[] = [
    new SecretScanner(clock),
    new PromptInjectionScanner(clock),
    new SuspiciousFilesScanner(clock),
    new DependencyScanner(clock),
    new LicenseScanner(clock),
  ];
  if (!config.security.extendedScanningEnabled) {
    return scanners;
  }
  scanners.push(
    new SkillspectorScanner(clock),
    new OssBinaryScanner("semgrep", "adapter-1.0.0", COST_CATALOG.semgrep, "semgrep", clock),
    new OssBinaryScanner("gitleaks", "adapter-1.0.0", COST_CATALOG.gitleaks, "gitleaks", clock),
    new OssBinaryScanner("trivy", "adapter-1.0.0", COST_CATALOG.trivy, "trivy", clock),
    new OssBinaryScanner("clamav", "adapter-1.0.0", COST_CATALOG.clamav, "clamscan", clock),
    new OssBinaryScanner("osv", "adapter-1.0.0", COST_CATALOG.osv, "osv-scanner", clock),
    new OssBinaryScanner("syft", "adapter-1.0.0", COST_CATALOG.syft, "syft", clock),
  );
  return scanners;
}
