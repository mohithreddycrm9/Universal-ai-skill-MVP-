# Scanner architecture

See [scanner-interface.md](./scanner-interface.md). `SecurityOrchestrator` runs enabled scanners concurrently. Adapters: secret, prompt_injection, suspicious_files, dependency (SBOM summary), license, SkillSpector, OSS CLIs, Snyk. Incremental scanning may narrow files but does **not** replace a full configured scan.


Federation: every executed scanner contributes; `requiredScanners`/`optionalScanners` govern coverage completeness only for required; optional absence is not a gap.
