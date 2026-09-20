# Universal Skill Trust Gateway

A software-agnostic **MCP gateway** that sits between AI agents and Agent Skills / MCP Skills / GitHub / vendor / enterprise registries / scanners.

It is not “an AI that downloads random skills from GitHub.” It is a security and trust control plane:

**DISCOVER → VERIFY → SECURITY-CHECK → SANDBOX → AUTHORIZE → CACHE → SERVE**

Verified capabilities are served with **progressive disclosure** (metadata first, SKILL.md only when needed). Repositories are never dumped into the model context.

Trust is not authorization. `INCONCLUSIVE` is not `PASS`. The gateway never claims a skill is universally “safe” or free of malware.

## Requirements

- Node.js 20.11+ (22+ recommended; SQLite uses `node:sqlite` with `--experimental-sqlite`)
- Optional: Docker for the container sandbox adapter (missing runtime ⇒ `INCONCLUSIVE`, not `PASS`)

## Setup

```bash
npm install
npm test
npm run build
```

## Run the MCP server (stdio)

```bash
npx skill-mcp serve
# or
npm run dev
```

Logs go to **stderr**. Do not capture stdout when a client uses stdio MCP.

## Optional HTTP

```bash
npx skill-mcp serve --http --host 127.0.0.1 --port 43177
```

- MCP: `POST http://127.0.0.1:43177/mcp`
- Health: `GET http://127.0.0.1:43177/health`

## CLI

```bash
npx skill-mcp discover "csv normalization"
npx skill-mcp acquire "csv-normalize" --wait --json
npx skill-mcp list --json
npx skill-mcp status skl_…
npx skill-mcp audit --json
```

## Connect a client

Examples: `examples/mcp-clients/cursor.json`, `claude-desktop.json`, `codex.toml`.

Point `command` at `node --experimental-sqlite /abs/path/dist/index.js serve`.

## Tests

```bash
npm test
npm run bench
```

Malicious fixtures under `examples/malicious-fixtures` are **simulated and non-destructive**. They are not executed on the host.

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
| [docs/local-development.md](docs/local-development.md) | Dev setup |
| [docs/production-deployment.md](docs/production-deployment.md) | Production notes |

## Configuration

Security-sensitive policy lives in `config/` (`trust-policy.yaml`, `security-policy.yaml`, `sandbox-policy.yaml`, `scanner-policy.yaml`, `registry.yaml`). Empty official/verified lists mean **no publisher is official**.

## License

Apache-2.0
