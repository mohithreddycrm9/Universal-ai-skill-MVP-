# Scanner adapters

Prefer OSS. Commercial adapters are optional and **cost-gated**. Missing scanners never become `PASS`.

| Adapter | Kind | Default | Missing binary |
| --- | --- | --- | --- |
| secret, prompt_injection, suspicious_files, dependency, license | Built-in, free, deterministic | enabled | n/a |
| strix | [usestrix/strix](https://github.com/usestrix/strix) OSS CLI (`strix --target`); never `strix cloud` | enabled | Missing binary/Docker/LLM → `ERROR` (never `PASS`) |
| semgrep, gitleaks, trivy, clamav, osv, syft | OSS CLI if present | disabled until you install them | `ERROR` (never `PASS`) |
| snyk | Commercial | disabled; never auto-fallback | `NOT_RUN` |

When an OSS binary is present, the adapter invokes it (offline flags where the tool supports them). A `PASS` is a configured-check outcome only, not a universal safety claim. Semgrep uses `config/semgrep-local.yml` rather than hosted `--config auto`. Trivy uses `--offline-scan --skip-db-update` (missing DB ⇒ `INCONCLUSIVE`/`ERROR`, not a silent download of unknown-cost data).


See [scanner-interface.md](./scanner-interface.md) and [COST_POLICY.md](./COST_POLICY.md).


Full STRIX notes: [STRIX.md](./STRIX.md).
