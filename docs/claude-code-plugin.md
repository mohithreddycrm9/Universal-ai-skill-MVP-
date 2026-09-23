# Claude Code plugin

This repository is a **Claude Code plugin** (manifest in `.claude-plugin/plugin.json`) with bundled MCP (`.mcp.json`) and skill (`skills/universal-skill-trust/`).

## Install from GitHub

In Claude Code:

```text
/plugin marketplace add mohithreddycrm9/Universal-ai-skill-MVP-
/plugin install universal-skill-trust@universal-skill-trust
/reload-plugins
```

Verify MCP: `/mcp` — you should see **universal-skill-trust**.

## Test locally (plugin dir)

From a clone of this repo:

```bash
npm install
npm run build
claude --plugin-dir "$(pwd)"
```

Or in an existing session:

```text
/plugin install /absolute/path/to/Universal-ai-skill-MVP-
```

First MCP start may run `npm install` and `npm run build` via `scripts/plugin-mcp-serve.mjs` (logs on stderr only).

## Skill

Invoke: `/universal-skill-trust:universal-skill-trust` (plugin name + skill name).

## MCP configuration

`.mcp.json` uses `${CLAUDE_PLUGIN_ROOT}` for config (`config/`) and data (`data/`). Do not put MCP servers only in `plugin.json` — use `.mcp.json` (Claude Code loads this reliably).

## Claude Desktop

For Claude Desktop (not Claude Code), see `examples/mcp-clients/claude-desktop.json` and point at `scripts/plugin-mcp-serve.mjs` with absolute paths.
