# Market-ready deployment

This gateway is designed for **controlled production**: you operate the MCP, configure trust allowlists, and accept that verification is **configured-check outcomes**, not universal safety.

## Can we use STRIX in the real world?

**Yes — for HIGH/CRITICAL-risk skills**, when you run the **local OSS** stack (not Strix Cloud):

| Requirement | Why |
| --- | --- |
| `strix` CLI ([usestrix/strix](https://github.com/usestrix/strix)) | Adapter runs `strix --target <quarantine>` |
| Docker engine | Upstream STRIX sandbox |
| LLM | `STRIX_LLM` — use **Ollama/lmstudio/localhost** under `ALLOW_FREE_ONLY`, or approve paid models |

Policy behavior:

- **LOW/MEDIUM risk** — STRIX is optional; if STRIX is down, acquisition can still **PASS** (other scanners must pass).
- **HIGH/CRITICAL risk** — STRIX becomes **required** (`strixRequiredForRisk`); missing/ERROR/NOT_RUN → **INCONCLUSIVE** (fail closed).

STRIX complements **SkillSpector** (skill-specific static patterns) and built-in scanners; it does **not** replace static-only sandbox (`SANDBOX_STATIC_ONLY` — no runtime detonation).

See [STRIX.md](./STRIX.md) for install and cost gating.

## Recommended production stack

| Layer | Market-ready choice |
| --- | --- |
| Gateway | `skill-mcp serve` (stdio behind agent) or HTTP on **loopback** + auth proxy |
| Registry | PostgreSQL (`DATABASE_URL`) for multi-user; SQLite for single-node pilots |
| Trust | Populate `trust-policy.yaml` verified/official publishers |
| Static scans | Built-in + **SkillSpector** (`skillspector` CLI) + **semgrep/gitleaks** optional |
| AI-assisted scan | **STRIX** for HIGH/CRITICAL only |
| Cost | Keep `ALLOW_FREE_ONLY` unless you operate an approval workflow |
| Sandbox | Docker static observer; executable skills need sandbox PASS |

## Quick start (production profile)

```bash
npm ci && npm run build
./scripts/use-market-ready-config.sh ./config-active
export SKILL_MCP_CONFIG_DIR=./config-active
export SKILL_MCP_DATA_DIR=./data
export STRIX_LLM=ollama/llama3.1:8b   # example — local/free
export SKILL_MCP_STRIX_LLM_IS_FREE=1  # only valid with local markers; see STRIX.md
./scripts/check-market-ready.sh
npx skill-mcp serve
```

Install external CLIs on the **same host** as the gateway (STRIX needs Docker; do not mount `docker.sock` into an untrusted container).

## Pre-flight check

```bash
./scripts/check-market-ready.sh
```

## What we still do not claim

- Malware-free or universally safe skills
- Runtime execution of untrusted entrypoints on the host
- Curated marketplace listing (Cursor/Claude directory review is separate)

Operators should ship logs to a SIEM, back up registry data, and run [security-audit-checklist.md](./security-audit-checklist.md).
