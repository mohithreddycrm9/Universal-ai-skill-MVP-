# Local setup (no payment information required)

## Fast path

```bash
npm install
npm test
npm run build
npx skill-mcp serve
```

You do **not** need a cloud account, SaaS key, paid scanner, or LLM. Data defaults to `./data` (gitignored SQLite). Config defaults to `./config`.

```bash
npx skill-mcp costs --json
npx skill-mcp approvals --json
```

## Registry

| Driver | How to enable | Notes |
| --- | --- | --- |
| SQLite (default) | `config/registry.yaml` `database.driver: sqlite` | `node:sqlite` + `--experimental-sqlite` |
| PostgreSQL (optional) | `database.driver: postgres` and `DATABASE_URL` (or `registry.database.url`) | Install optional `pg` (`npm i pg`). Missing URL fails closed — it will not silently use SQLite. |

Live Postgres tests: `DATABASE_URL=postgres://… npm test`.

## Discovery

Enabled by default (all free/local):

- `github` — public REST only; private/enterprise is unknown-cost and needs approval
- `mcp_registry` — `config/mcp-registry.fixture.yaml` (remote APIs stay fail-closed unless clearly free)
- `official_vendor` / `enterprise_registry` — YAML allowlists, no vendor names in core

## Optional binaries (never required)

Missing tools are `ERROR` / `NOT_RUN` / `INCONCLUSIVE`, **never `PASS`**.

| Binary | Purpose |
| --- | --- |
| Docker or Podman | Isolated verification sandbox (`SKILL_MCP_SANDBOX=docker`, default). Image pull is off; missing image ⇒ `INCONCLUSIVE`. |
| `strix` | Optional STRIX CLI |
| `semgrep` | Local SAST using `config/semgrep-local.yml` (offline; not `--config auto`) |
| `gitleaks` | Local secret scan |
| `trivy` | Offline fs scan (`--skip-db-update`) |
| `clamscan` | ClamAV |
| `osv-scanner` | Offline OSV |
| `syft` | Local SBOM |

Enable extras in `config/scanner-policy.yaml` after installing them. Commercial scanners stay disabled and cost-gated.

Cloud/hosted sandbox is **off** (`cloudSandboxEnabled: false`) and is not a silent fallback.

See [local-development.md](./local-development.md) and [COST_POLICY.md](./COST_POLICY.md).
