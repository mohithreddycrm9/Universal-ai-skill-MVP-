# Market-ready deployment

This gateway is designed for **controlled production**: you operate the MCP, configure trust allowlists, and accept that verification is **configured-check outcomes**, not universal safety.

**No LLM-backed pentest adapters** are bundled (enterprise-friendly). Use **static** scanners only: built-ins, **NVIDIA SkillSpector** (`--no-llm` by default), Semgrep, Gitleaks, etc.

## Recommended production stack

| Layer | Market-ready choice |
| --- | --- |
| Gateway | `skill-mcp serve` (stdio behind agent) or HTTP on **loopback** + auth proxy |
| Registry | PostgreSQL (`DATABASE_URL`) for multi-user; SQLite for single-node pilots |
| Trust | Populate `trust-policy.yaml` verified/official publishers |
| Skill-focused static scan | **SkillSpector** (`skillspector` CLI, `useLlm: false`) |
| General SAST / secrets | Semgrep, Gitleaks (optional OSS CLIs) |
| Cost | Keep `ALLOW_FREE_ONLY`; do not enable `skillspector.useLlm` without approval workflow |
| Sandbox | Docker static observer; executable skills need sandbox PASS |

## Quick start (production profile)

```bash
npm ci && npm run build
./scripts/use-market-ready-config.sh ./config-active
export SKILL_MCP_CONFIG_DIR=./config-active
export SKILL_MCP_DATA_DIR=./data
./scripts/check-market-ready.sh
npx skill-mcp serve
```

Install external CLIs on the **same host** as the gateway. Do not mount `docker.sock` into an untrusted MCP container.

## Pre-flight check

```bash
./scripts/check-market-ready.sh
```

## What we still do not claim

- Malware-free or universally safe skills
- Runtime execution of untrusted entrypoints on the host
- Curated marketplace listing (Cursor/Claude directory review is separate)

Operators should ship logs to a SIEM, back up registry data, and run [security-audit-checklist.md](./security-audit-checklist.md).
