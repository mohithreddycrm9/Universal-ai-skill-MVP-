# Cursor plugin

This repository ships as a **Cursor Plugin** (manifest in `.cursor-plugin/plugin.json`) with a bundled MCP server (`mcp.json`).

## Install from the marketplace

After publication, install **Universal Skill Trust Gateway** from [Customize](https://cursor.com/docs/customize-cursor) or the [Cursor Marketplace](https://cursor.com/marketplace).

## Test locally

1. Build once (optional — the plugin launcher builds on first MCP start):

   ```bash
   npm install
   npm run build
   ```

2. Copy or clone into Cursor’s local plugins folder (symlinks to paths **outside** `~/.cursor/plugins/local` are ignored by Cursor):

   ```bash
   mkdir -p ~/.cursor/plugins/local
   rsync -a --exclude node_modules --exclude dist --exclude data ./ ~/.cursor/plugins/local/universal-skill-trust/
   cd ~/.cursor/plugins/local/universal-skill-trust && npm install && npm run build
   ```

3. Reload the Cursor window (**Developer: Reload Window**).

4. Open **Customize** → confirm **universal-skill-trust** MCP server and the **universal-skill-trust** skill.

> **Enterprise:** admins may need to enable **Allow Local Plugin Imports** under Dashboard → Settings → Security & Identity → Marketplace and Plugins.

## MCP configuration

`mcp.json` starts the gateway via `scripts/plugin-mcp-serve.mjs`, which installs dependencies and builds `dist/` if missing. Config and data directories default to `${CURSOR_PLUGIN_ROOT}/config` and `${CURSOR_PLUGIN_ROOT}/data`.

For manual MCP setup without the plugin, see `examples/mcp-clients/cursor.json`.

## Publish

Submit the public GitHub repository at [cursor.com/marketplace/publish](https://cursor.com/marketplace/publish). Plugins must be open source and are manually reviewed.
