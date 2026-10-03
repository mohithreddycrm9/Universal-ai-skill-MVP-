# Codex plugin

Universal Skill Trust Gateway bundles the existing skill and local stdio MCP
server. It discovers and supplies skill knowledge; other authorized tools perform
product operations. No new backend, paid model, or cloud deployment is required.

## Requirements

- Node.js 22.13+ or 24+ with `node:sqlite`, npm, and Git.
- A Codex client with plugin marketplace support for the plugin route. Manual MCP
  setup is available for clients without that support.
- Optional scanners retain their existing requirements. Missing checks remain
  `INCONCLUSIVE`, `ERROR`, or `NOT_RUN`; a successful connection is not a safety verdict.

## Test this checkout

Run from the repository root:

```bash
npm ci
npm run build
npm run validate:codex-plugin
codex plugin marketplace add .
codex plugin marketplace list
```

Open the ChatGPT desktop Plugins Directory, choose **Universal AI Skills**, and
install **Universal Skill Trust Gateway**. After changing a local plugin, refresh
the marketplace and restart the desktop app so its installed copy picks up changes.
The root `.agents/plugins/marketplace.json` resolves `source.path: "./"` from
the repository root, not from the catalog directory.

Once these changes are merged, the remote marketplace can be added with:

```bash
codex plugin marketplace add mohithreddycrm9/Universal-ai-skill-MVP-
```

This is repo distribution, not publication to the public Plugins Directory.
These desktop steps do not install a plugin on an iPhone.

## Packaging and startup

The supported compatibility layout is intentional: `.codex-plugin/plugin.json`
declares `./skills/` and `./.mcp.json`, reusing the Claude-compatible MCP definition.
The existing Cursor `mcp.json` is left intact. Do not rename it into a portable
Agent Plugins manifest without also converting its schema and transport fields.

Codex provides the `CLAUDE_PLUGIN_ROOT` compatibility alias used in `.mcp.json`.
The launcher also accepts `PLUGIN_ROOT`, then the Claude/Cursor root variables,
then its own location, so it does not rely on the client's working directory.
Dependencies are installed with `npm ci` and missing build artifacts are compiled;
bootstrap output goes to stderr to keep stdout available for MCP. Prebuild the
installed copy when startup deadlines or restricted network access prevent bootstrap.

Config defaults to `<plugin-root>/config`. Runtime data resolves in this order:
`SKILL_MCP_DATA_DIR`, `PLUGIN_DATA`, `CLAUDE_PLUGIN_DATA`, `<plugin-root>/data`.
Codex's writable plugin data directory keeps SQLite, approvals, and cache outside
the versioned installed package. Operator commands must use the same config/data
environment variables as the server, or they will inspect a different registry.

No secrets are bundled. Configure any optional credentials locally. Skill
discovery can contact configured sources; this package does not guarantee that
every existing adapter keeps all data local. Review adapters before enabling them.

## Manual MCP alternative

Merge `examples/mcp-clients/codex.toml` into `~/.codex/config.toml`, replacing all
`/ABS/PATH` placeholders with your actual absolute repository path. Keep unrelated
configuration intact. Run `npm ci && npm run build` first, then `codex mcp list`
and start a new session. Do not configure this alongside the bundled plugin MCP.
Manual MCP setup provides tools but does not install the bundled workflow skill.

## Check behavior

Ask: "Use Universal Skill Trust to find a CSV normalization skill. Show its
provenance and effective permissions before loading its instructions."

Expect discovery candidates, acquisition/status reporting, trust/permission
evidence, and progressive `get_skill` responses when disclosure gates allow them.
Pending paid-scanner or capability approvals require the user's operator action.
Verification never grants permission to execute retrieved scripts.

`npm run validate:codex-plugin` builds the gateway and tests actual stdio startup,
tool listing/calls, startup from an unrelated directory, plugin data isolation,
and manifest paths. It does not test the installed Codex UI; complete that check
on a supported client.

Official references:
- https://developers.openai.com/plugins/build/plugins
- https://developers.openai.com/codex/mcp
