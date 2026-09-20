# Local development

## Requirements

- Node.js 20.11+ (22+ recommended; this repo uses `node:sqlite`, currently behind `--experimental-sqlite`)
- npm 10+
- Optional: Docker Engine for the container sandbox adapter

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

Logs go to stderr. Do not redirect stdout if a client is using stdio MCP.

## Run with HTTP (operators / debugging)

```bash
npx skill-mcp serve --http --host 127.0.0.1 --port 43177
```

- MCP: `POST http://127.0.0.1:43177/mcp`
- Health: `GET http://127.0.0.1:43177/health`

## CLI

```bash
npx skill-mcp discover "csv normalization"
npx skill-mcp list --json
npx skill-mcp status skl_…
npx skill-mcp audit --json
```

Data directory defaults to `./data` (gitignored). Override with `SKILL_MCP_DATA_DIR`.

Config directory defaults to `./config`. Override with `SKILL_MCP_CONFIG_DIR`.

## Tests

```bash
npm test                 # unit + integration + security
npm run bench            # small local perf bench
```

Malicious fixtures under `examples/malicious-fixtures` are **simulated and non-destructive**. Tests never execute them on the host.
