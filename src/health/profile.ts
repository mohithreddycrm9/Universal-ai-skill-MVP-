/** Built-in scanners used for local workspace / repo health checks (no STRIX, no paid tools). */
export const CODE_HEALTH_SCANNER_IDS = [
  "secret",
  "prompt_injection",
  "suspicious_files",
  "dependency",
  "license",
  "code_health",
] as const;

export const CODE_HEALTH_REQUIRED_SCANNER_IDS = [
  "secret",
  "prompt_injection",
  "suspicious_files",
  "dependency",
  "code_health",
] as const;
