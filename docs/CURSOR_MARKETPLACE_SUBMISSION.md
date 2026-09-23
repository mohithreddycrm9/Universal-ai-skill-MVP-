# Cursor Marketplace submission (copy-ready)

Official form: [cursor.com/marketplace/publish](https://cursor.com/marketplace/publish) (must be signed into Cursor).

Alternate path (documented in [cursor/plugin-template](https://github.com/cursor/plugin-template)): email **kniparko@anysphere.com** with the fields below.

## Listing fields

| Field | Value |
| --- | --- |
| Plugin name | `universal-skill-trust` |
| Display name | Universal Skill Trust Gateway |
| Version | `0.1.0` |
| Short description (form) | See **Descriptions** below — use the **Short** block unless the form asks for more. |
| License | Apache-2.0 |
| GitHub repository | `https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-` |
| Plugin manifest | `.cursor-plugin/plugin.json` |
| MCP config | `mcp.json` (stdio via `scripts/plugin-mcp-serve.mjs`) |
| Skill | `skills/universal-skill-trust/SKILL.md` |
| Logo (1:1 SVG, background plate) | `assets/logo.svg` |
| **Logo URL** (paste in form) | `https://raw.githubusercontent.com/mohithreddycrm9/Universal-ai-skill-MVP-/main/assets/logo.svg` |

## One-tap email (mobile / desktop)

[Open pre-filled submission email](mailto:kniparko@anysphere.com?subject=Marketplace%20plugin%20submission%3A%20universal-skill-trust&body=Hi%2C%0A%0APlease%20review%20this%20plugin%20for%20the%20Cursor%20Marketplace.%0A%0ARepository%3A%20https%3A%2F%2Fgithub.com%2Fmohithreddycrm9%2FUniversal-ai-skill-MVP-%0APlugin%20name%3A%20universal-skill-trust%0ADisplay%20name%3A%20Universal%20Skill%20Trust%20Gateway%0ALicense%3A%20Apache-2.0%0A%0AManifest%3A%20.cursor-plugin%2Fplugin.json%0AMCP%3A%20mcp.json%20%2B%20scripts%2Fcursor-mcp-serve.mjs%0ASkill%3A%20skills%2Funiversal-skill-trust%2FSKILL.md%0A%0AThanks%2C)

## Descriptions (copy-paste)

**Short** (tagline / ~160 characters):

```text
Discover, acquire, and serve third-party agent skills safely. MCP gateway with OSS-first scanning, quarantine lifecycle, and compact skill delivery—no product APIs.
```

**Standard** (most marketplace description fields):

```text
Universal Skill Trust Gateway is an MCP server that helps Cursor agents find, acquire, verify, and load third-party skills without dumping full repositories into context.

Workflow: discover candidates → async acquire → security scans and static sandbox checks → capability firewall → serve compact SKILL manifests with progressive disclosure.

Built free/OSS-first (local SQLite, local scanners, optional Docker). Paid or unknown-cost tools require explicit human approval. Trust evidence is separate from authorization; INCONCLUSIVE is not PASS. Static-only sandbox stage—skill entrypoints are not executed on the host during verification.

Does not run Terraform, AWS, or other product APIs—only skill trust and delivery.
```

**Extended** (if the form allows a longer blurb):

```text
Agents often need reusable “how to work with X” knowledge (skills), but pulling random repos into chat is risky. This plugin bundles an MCP gateway plus an agent skill that teaches the discover → acquire → verify → get_skill workflow.

The gateway enforces a quarantine-first lifecycle: pin commits, run configured OSS scanners, cache verification results, and expose only compact, schema-valid manifests. A capability firewall decides effective permissions—declared skill prose is advisory only.

Operators stay in control: default cost policy is ALLOW_FREE_ONLY ($0); metered scanners and cloud sandboxes stay off unless approved. Results describe configured-check outcomes, not universal safety.

Open source (Apache-2.0). Repository includes policy YAML, CLI for approvals/audit, and local test fixtures.
```

## Web form steps

1. Sign in at [cursor.com/marketplace/publish](https://cursor.com/marketplace/publish).
2. Paste repository URL: `https://github.com/mohithreddycrm9/Universal-ai-skill-MVP-`
3. Click **Submit application**.
