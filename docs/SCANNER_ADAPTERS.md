# Scanner adapters

Prefer OSS. Commercial adapters are optional and **cost-gated**. Missing scanners never become `PASS`.

| Adapter | Kind | Default |
| --- | --- | --- |
| secret, prompt_injection, suspicious_files, dependency, license | Built-in, free, deterministic | enabled |
| strix | OSS CLI if present | enabled; ERROR if missing |
| semgrep, gitleaks, trivy, clamav, osv, syft | OSS CLI if present | disabled until you install them |
| snyk | Commercial | disabled; never auto-fallback |

See [scanner-interface.md](./scanner-interface.md) and [COST_POLICY.md](./COST_POLICY.md).
