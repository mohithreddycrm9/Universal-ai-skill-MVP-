# Scanner adapters

Prefer OSS. Commercial adapters are optional and **cost-gated**. Missing scanners never become `PASS`.

| Adapter | Kind | Default | Missing binary |
| --- | --- | --- | --- |
| secret, prompt_injection, suspicious_files, dependency, license | Built-in, free, deterministic | enabled | n/a |
| skillspector | [NVIDIA/SkillSpector](https://github.com/NVIDIA/SkillSpector) (`skillspector scan --no-llm`) | disabled until CLI installed | Missing binary → `ERROR`; see [SKILLSPECTOR.md](./SKILLSPECTOR.md) |
| semgrep, gitleaks, trivy, clamav, osv, syft | OSS CLI if present | disabled until you install them | `ERROR` (never `PASS`) |

When an OSS binary is present, the adapter invokes it (offline flags where the tool supports them). A `PASS` is a configured-check outcome only, not a universal safety claim. Semgrep uses `config/semgrep-local.yml` rather than hosted `--config auto`. Trivy uses `--offline-scan --skip-db-update` (missing DB ⇒ `INCONCLUSIVE`/`ERROR`, not a silent download of unknown-cost data).


See [scanner-interface.md](./scanner-interface.md) and [COST_POLICY.md](./COST_POLICY.md).


SkillSpector notes: [SKILLSPECTOR.md](./SKILLSPECTOR.md).
