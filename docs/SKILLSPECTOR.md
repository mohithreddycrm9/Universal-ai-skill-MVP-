# SkillSpector adapter

[NVIDIA SkillSpector](https://github.com/NVIDIA/SkillSpector) (Apache-2.0) is wired as optional scanner `skillspector` in the security orchestrator.

## Install CLI

```bash
pip install 'git+https://github.com/NVIDIA/skillspector.git'
# or: uv tool install 'skillspector @ git+https://github.com/NVIDIA/skillspector.git'
```

Ensure `skillspector` is on `PATH` (e.g. `~/.local/bin`).

## Policy

`config/scanner-policy.yaml`:

- `useLlm: false` (default) — runs `skillspector scan <quarantine> --no-llm --format json` ($0).
- `useLlm: true` — semantic analyzers; gated like STRIX under `ALLOW_FREE_ONLY` (`scan_skill:skillspector_llm`).

`config/security-policy.yaml` lists `skillspector` under `optionalScanners` (missing binary → `ERROR`, not a coverage gap for required scanners).

## Baseline

Optional `baselinePath` in scanner config points at a SkillSpector baseline file for suppressing known findings.

## Not PASS

SkillSpector `FAIL` / `DO_NOT_INSTALL` / HIGH findings feed the same federation rules as other scanners. Results are configured-check outcomes only.
