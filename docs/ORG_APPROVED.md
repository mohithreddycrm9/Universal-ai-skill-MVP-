# Organization-approved scope

Default MCP behavior matches what most security teams approve **without extra vendor review**:

## Included (default)

| Component | Notes |
| --- | --- |
| Built-in scanners | `secret`, `prompt_injection`, `suspicious_files`, `dependency`, `license` |
| Local registry / fixtures | `LocalSource`, YAML catalogs on disk |
| In-process sandbox | Static observation only (`SANDBOX_STATIC_ONLY`) |
| Cost policy | `ALLOW_FREE_ONLY` — no paid cloud APIs |
| GitHub remote | **Off** by default (`registry.yaml`); enable only with org approval + token |

## Not in default gateway

| Removed / gated | Reason |
| --- | --- |
| STRIX / AI pentest | LLM + Docker; not enterprise-default |
| Snyk / commercial scanners | Removed from product surface |
| SkillSpector LLM | Removed — static `--no-llm` only when extended mode is on |
| External CLIs | Semgrep, Gitleaks, SkillSpector, … only when `extendedScanningEnabled: true` |

## Enable optional OSS (after security sign-off)

```bash
./scripts/use-market-ready-config.sh ./config-active
# sets extendedScanningEnabled: true + SkillSpector / Semgrep / Gitleaks
export SKILL_MCP_CONFIG_DIR=./config-active
```

Or add to `security-policy.yaml`:

```yaml
extendedScanningEnabled: true
```

and enable scanners in `scanner-policy.yaml`.

## Deploy checklist

```bash
npm ci && npm run build
./scripts/check-market-ready.sh
export SKILL_MCP_CONFIG_DIR=config   # or ./config-active
npx skill-mcp serve
```

Fill `trust-policy.yaml` before approving unknown publishers in production.
