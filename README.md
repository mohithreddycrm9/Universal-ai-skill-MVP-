# Universal Skills MCP

A software-agnostic **MCP** that lets an agent discover, acquire, verify, cache, and **serve skills** — how to work with a technology. It does **not** execute Terraform, AWS, ServiceNow, or other product APIs. Other tools/MCPs do that.

> Current sandbox stage: `ISOLATED_STATIC` / `SANDBOX_STATIC_ONLY` — static observation only; does not execute skill code/entrypoints/hooks (not runtime detonation).

**DISCOVER → VERIFY → SECURITY-CHECK → SANDBOX → AUTHORIZE → CACHE → SERVE**

Core is **free/open-source-first**: local SQLite, local/OSS scanners, local sandbox. No cloud account or paid API is required. Any potentially billable adapter needs **explicit human approval**. Paid scanners never run as a silent fallback.

Trust is not authorization. `INCONCLUSIVE` is not `PASS`. The default gateway ships **organization-approved built-in scanners only** (no commercial tools, no LLM scanners, no external CLIs until you opt in). See [docs/ORG_APPROVED.md](./docs/ORG_APPROVED.md).

## Production

- Default: [docs/ORG_APPROVED.md](./docs/ORG_APPROVED.md) + `./scripts/check-market-ready.sh`
- Optional OSS (SkillSpector, Semgrep, Gitleaks): [docs/MARKET_READY.md](./docs/MARKET_READY.md) + `./scripts/use-market-ready-config.sh`

## Fast path

```bash
npm install
npm test
npm run build
npm run validate          # typecheck + build + test (includes e2e)
npx skill-mcp serve
```

E2E lifecycle scenarios A–N (local fixtures): `npm run test:e2e` — see [docs/E2E.md](./docs/E2E.md). Docker/network optional; live GitHub fetches are NOT_EXECUTED unless you run them yourself.

Optional HTTP:

```bash
npx skill-mcp serve --http --host 127.0.0.1 --port 43177
```

Cost catalog and approvals (still required before any metered/unknown-cost work):

```bash
npx skill-mcp costs --json
npx skill-mcp approvals --json
npx skill-mcp approve <approvalId>
npx skill-mcp reject <approvalId>
```

## Requirements

- Node.js 20.11+ (22+ recommended; SQLite uses `node:sqlite` with `--experimental-sqlite`)
- Optional runtimes — missing tools never count as `PASS`:

| Optional | Used for | If missing |
| --- | --- | --- |
| Docker or Podman | Isolated verification sandbox | `INCONCLUSIVE` |
| `pg` + `DATABASE_URL` | PostgreSQL registry | SQLite stays default |
| [SkillSpector](https://github.com/NVIDIA/SkillSpector), Semgrep, Gitleaks, … | Only when `extendedScanningEnabled: true` | `ERROR` / `NOT_RUN` if enabled but missing |

Local Docker is treated as free. Cloud/hosted sandboxes stay **off** and approval-gated.

## Run the MCP server (stdio)

```bash
npx skill-mcp serve
# or
npm run dev
```

Logs go to **stderr**. Do not capture stdout when a client uses stdio MCP.

- MCP HTTP: `POST http://127.0.0.1:43177/mcp`
- Health: `GET http://127.0.0.1:43177/health`

## CLI

```bash
npx skill-mcp discover "csv normalization"
npx skill-mcp acquire "csv-normalize" --wait --json
npx skill-mcp list --json
npx skill-mcp status skl_…
npx skill-mcp audit --json
npx skill-mcp costs --json
npx skill-mcp approvals --json
```

## Claude Code plugin

Install as a Claude Code plugin (MCP + skill bundled):

- Manifest: `.claude-plugin/plugin.json` and `marketplace.json`
- MCP: `.mcp.json` (uses `${CLAUDE_PLUGIN_ROOT}` via `scripts/plugin-mcp-serve.mjs`)
- Install: [docs/claude-code-plugin.md](./docs/claude-code-plugin.md)
- Community directory submit: [docs/CLAUDE_MARKETPLACE_SUBMISSION.md](./docs/CLAUDE_MARKETPLACE_SUBMISSION.md)

## Cursor plugin

Install as a Cursor Plugin (MCP + skill bundled):

- Manifest: `.cursor-plugin/plugin.json`
- MCP: `mcp.json` (uses `${CURSOR_PLUGIN_ROOT}` and auto-build via `scripts/plugin-mcp-serve.mjs`)
- Local test and publish steps: [docs/cursor-plugin.md](./docs/cursor-plugin.md)

## Connect a client

Examples: `examples/mcp-clients/cursor.json`, `claude-desktop.json`, `codex.toml`.

For Cursor, prefer the plugin above. For raw MCP config, point `command` at `node --experimental-sqlite /abs/path/dist/index.js serve` (or the plugin launcher script).

## Tests

```bash
npm test
npm run bench
```

Malicious fixtures under `examples/malicious-fixtures` are **simulated and non-destructive**. They are not executed on the host.

Postgres live tests run only when `DATABASE_URL` is set.

## Docs

| Doc | Topic |
| --- | --- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Gateway architecture |
| [docs/ARCHITECTURE-MIGRATION.md](docs/ARCHITECTURE-MIGRATION.md) | Old → new mapping |
| [docs/SECURITY.md](docs/SECURITY.md) | Security model |
| [docs/THREAT-MODEL.md](docs/THREAT-MODEL.md) | Threat model |
| [docs/TRUST-MODEL.md](docs/TRUST-MODEL.md) | Trust broker |
| [docs/SKILL-LIFECYCLE.md](docs/SKILL-LIFECYCLE.md) | Quarantine-first lifecycle |
| [docs/CAPABILITY-MODEL.md](docs/CAPABILITY-MODEL.md) | Authorization firewall |
| [docs/SCANNER-ARCHITECTURE.md](docs/SCANNER-ARCHITECTURE.md) | Pluggable scanners |
| [docs/SANDBOX.md](docs/sandbox.md) | Isolation |
| [docs/PERFORMANCE.md](docs/PERFORMANCE.md) | Cache, parallel scan, tokens |
| [docs/COST_POLICY.md](docs/COST_POLICY.md) | Free-first + human cost approval |
| [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md) | Clone-and-run without payment |
| [docs/SCANNER_ADAPTERS.md](docs/SCANNER_ADAPTERS.md) | OSS vs commercial scanners |
| [docs/SKILLSPECTOR.md](docs/SKILLSPECTOR.md) | NVIDIA SkillSpector scanner adapter |
| [docs/local-development.md](docs/local-development.md) | Dev setup |
| [docs/production-deployment.md](docs/production-deployment.md) | Production notes |

## Configuration

Security-sensitive policy lives in `config/` (`trust-policy.yaml`, `security-policy.yaml`, `sandbox-policy.yaml`, `scanner-policy.yaml`, `registry.yaml`, `cost-policy.yaml`). Empty official/verified lists mean **no publisher is official**. Default cost policy is `ALLOW_FREE_ONLY` ($0 — paid/unknown denied).

Discovery sources are config-driven allowlists (YAML). Core does not hard-code product vendors. The MCP registry adapter is fixture-backed by default; unknown-cost remotes fail closed.

## License

Apache-2.0
